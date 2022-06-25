package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    private static final String REPLACEMENT = "***";

    private TrieNode root = new TrieNode();

    @PostConstruct
    public void init() {
        try (
                InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            String keyWord;
            while ((keyWord = reader.readLine()) != null) {
                this.addKeyWord(keyWord);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败!" + e.getMessage());
        }

    }

    // 将一个敏感词添加至前缀树
    private void addKeyWord(String keyWord) {
        char[] ch = keyWord.toCharArray();
        TrieNode cur = root;
        for (int i = 0; i < ch.length; i++) {
            if (cur.nexts.get(ch[i]) == null) {
                cur.nexts.put(ch[i], new TrieNode());
            }
            cur = cur.nexts.get(ch[i]);
            if (i == ch.length - 1) {
                cur.setKeyWordEnd(true);
            }
        }
    }

    private class TrieNode {
        private boolean isKeyWordEnd = false;
        private Map<Character, TrieNode> nexts = new HashMap<>();

        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }

        public void insert(Character c, TrieNode node) {
            nexts.put(c, node);
        }

        public TrieNode getNext(Character c) {
            return nexts.get(c);
        }
    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 过滤敏感词
     *
     * @param text
     * @return
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        TrieNode curNode = root;
        int begin = 0;
        int position = 0;
        StringBuilder builder = new StringBuilder();
        while (begin < text.length()) {
            if (position < text.length()) {
                char c = text.charAt(position);
                // 跳过符号
                if (isSymbol(c)) {
                    if (curNode == root) {
                        builder.append(c);
                        begin++;
                    }
                    position++;
                } else {
                    // 检查下级结点
                    curNode = curNode.getNext(c);
                    if (curNode == null) {
                        builder.append(text.charAt(begin));
                        position = ++begin;
                        curNode = root;
                    } else if (curNode.isKeyWordEnd()) {
                        builder.append(REPLACEMENT);
                        begin = ++position;
                        curNode = root;
                    } else {
                        position++;
                    }
                }
            } else {
                builder.append(text.charAt(begin));
                position = ++begin;
                curNode = root;
            }
        }

        builder.append(text.substring(begin));
        return builder.toString();
    }

}
