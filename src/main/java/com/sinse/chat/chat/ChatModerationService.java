
package com.sinse.chat.chat;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import com.sinse.chat.domain.ChatModeration;
import com.sinse.chat.repository.ChatModerationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatModerationService {

    private final ChatModerationRepository moderationRepository;
    private volatile AhoCorasickDoubleArrayTrie<String> trie;
    private final List<String> baseTerms = new ArrayList<>();

    @PostConstruct
    public void init() throws IOException {
        // 1) 사전 로드
        loadWordList("badwords/badwords_ko.txt");
        loadWordList("badwords/badwords_en.txt");

        // 2) 변형(aliases) 확장
        List<String> expanded = expandVariants(baseTerms);

        // 3) 트라이 빌드 (아호코라식)
        buildTrie(expanded);
    }

    private void loadWordList(String path) throws IOException {
        try (var is = getClass().getClassLoader().getResourceAsStream(path);
             var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                baseTerms.add(normalizeBare(line));
            }
        }
    }
    private void buildTrie(List<String> terms) {
        Map<String, String> dict = new HashMap<>();
        for (String t : terms) dict.put(t, t);

        this.trie = new AhoCorasickDoubleArrayTrie<>();
        trie.build(dict);   // 여기서 바로 build(dict) 호출
    }

    public boolean isBanned(int roomId, int userId) {
        return moderationRepository.existsById_RoomIdAndId_UserIdAndId_Type(roomId, userId, ChatModeration.ModerationType.BAN);
    }

    /** 메시지 정화/차단 결과 */
    public ModerationResult moderate(String raw) {
        if (raw == null || raw.isBlank())
            return ModerationResult.blocked("empty");

        // 1) HTML/XSS 제거
        String cleanedHtml = Jsoup.clean(raw, org.jsoup.safety.Safelist.none());

        // 2) 강력 정규화 (전각→반각, 소문자, 공백/특수문자 축소, 한글 정규화)
        String norm = normalizeForMatch(cleanedHtml);

        // 3) 매칭
        List<AhoCorasickDoubleArrayTrie.Hit<String>> hits = trie.parseText(norm);
        if (hits.isEmpty()) {
            return ModerationResult.allowed(cleanedHtml);
        }

        // 4) 치환 (원문에서 위치 찾기 대신 간단히 전체 단어 마스킹)
        String masked = maskByDictionary(cleanedHtml);

        // 욕설이 감지되었으므로, 마스킹된 내용을 포함하되 allowed는 false로 반환
        return new ModerationResult(false, masked, "PROFANITY_DETECTED");
    }

    // ==================== 유틸 ====================

    /** 전처리: NFKC, lower, 공백축소, 특수문자 제거(문자·숫자만 유지), 한글 호환자모 흡수 */
    private String normalizeForMatch(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC);
        n = n.toLowerCase(Locale.ROOT);
        // 흔한 leet를 기본 치환
        n = n.replace('0', 'o').replace('1', 'i').replace('3', 'e')
                .replace('4', 'a').replace('5', 's').replace('7', 't')
                .replace('@', 'a').replace('$', 's').replace('!', 'i');

        // 문자 사이에 끼워 넣은 특수문자/공백 제거 (가볍게)
        n = n.replaceAll("[^\\p{L}\\p{Nd}]+", "");

        // 한글 호환자모를 일반 글자로 끌어당기는 최소 처리 (간단 버전)
        // 필요시 오픈코리안텍스트 등으로 더 강하게 처리
        return n;
    }

    /** 사전에 넣을 때는 공백/특수문자 제거 정도만 (매칭과 동일한 규칙) */
    private String normalizeBare(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        n = n.replaceAll("\s+", "");
        return n;
    }

    /** 간단한 변형 확장: leet / 자주 끼우는 특수문자 패턴 */
    private List<String> expandVariants(List<String> base) {
        List<String> out = new ArrayList<>(base.size() * 6);
        for (String w : base) {
            out.add(w);
            out.add(w.replace("a", "@"));
            out.add(w.replace("s", "$"));
            out.add(w.replace("i", "1"));
            out.add(w.replace("e", "3"));
            out.add(w.replace("o", "0"));
            // 필요하면 더 추가
        }
        // 중복 제거
        return out.stream().distinct().toList();
    }

    /** 아주 단순한 마스킹(실서비스는 위치매핑/부분마스킹을 더 정교화) */
    private String maskByDictionary(String original) {
        String tmp = original;
        for (String w : baseTerms) {
            if (w.length() < 2) continue;
            tmp = tmp.replaceAll("(?i)" + Pattern.quote(w), "*".repeat(Math.min(6, w.length())));
        }
        return tmp;
    }

    // 결과 객체
    public record ModerationResult(boolean allowed, String cleaned, String reason) {
        public static ModerationResult allowed(String cleaned) { return new ModerationResult(true, cleaned, null); }
        public static ModerationResult blocked(String reason)   { return new ModerationResult(false, null, reason); }
    }
}
