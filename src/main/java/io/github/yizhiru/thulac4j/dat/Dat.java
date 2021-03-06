package io.github.yizhiru.thulac4j.dat;

import io.github.yizhiru.thulac4j.common.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Double Array Trie (DAT).
 */
public class Dat implements Serializable {

    private static final long serialVersionUID = 8713857561296693244L;

    public static final int MATCH_FAILURE_INDEX = -1;

    /**
     * List of entries.
     */
    protected List<Entry> entries;

    /**
     * An entry contains base and check value.
     */
    public static class Entry implements Serializable {

        private static final long serialVersionUID = 8485610155599791207L;

        public int base;
        public int check;

        public Entry(int base, int check) {
            this.base = base;
            this.check = check;
        }

        @Override
        public String toString() {
            return base + " " + check;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry entry = (Entry) o;
            return base == entry.base && check == entry.check;
        }
    }

    public Dat(List<Entry> entries) {
        this.entries = entries;
    }

    public Dat() {
        this.entries = new ArrayList<>();
    }

    /**
     * 序列化.
     *
     * @param path 文件路径
     */
    public void serialize(String path) throws IOException {
        int[] arr = new int[2 * entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            arr[2 * i] = entries.get(i).base;
            arr[2 * i + 1] = entries.get(i).check;
        }
        FileChannel channel = new FileOutputStream(path).getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * (arr.length + 1));
        IntBuffer intBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                .asIntBuffer();
        intBuffer.put(arr.length);
        intBuffer.put(arr);
        channel.write(byteBuffer);
        channel.close();
    }

    /**
     * 加载序列化DAT模型
     *
     * @param path 文件目录
     * @return DAT模型
     */
    public static Dat loadDat(String path) throws IOException {
        return loadDat(new FileInputStream(path));
    }

    /**
     * 加载序列化DAT模型
     *
     * @param inputStream 文件输入流
     * @return DAT模型
     */
    public static Dat loadDat(InputStream inputStream) throws IOException {
        int[] array = IOUtils.toIntArray(inputStream);
        int arrayLen = array[0];
        List<Dat.Entry> entries = new ArrayList<>(arrayLen / 2);
        for (int i = 1; i < arrayLen; i += 2) {
            entries.add(new Dat.Entry(array[i], array[i + 1]));
        }
        return new Dat(entries);
    }

    /**
     * The size of DAT.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Get the entry by index.
     */
    public Entry get(int index) {
        return entries.get(index);
    }

    /**
     * 按照DAT的转移方程进行转移: ROOT_PATH[r] + c = s, check[s] = r
     *
     * @param r 前缀在DAT中的index
     * @param c 转移字符的index
     * @return 在DAT中的index，若不在则为-1
     */
    public int transition(int r, int c) {
        if (r < 0 || r >= entries.size()) {
            return MATCH_FAILURE_INDEX;
        }
        int s = entries.get(r).base + c;
        if (s >= entries.size() || entries.get(s).check != r) {
            return MATCH_FAILURE_INDEX;
        }
        return s;
    }

    /**
     * 词是否在trie树中
     *
     * @param word 词
     * @return 若存在，则为true
     */
    public boolean isWordMatched(String word) {
        return isWordMatched(-match(word));
    }

    /**
     * 词是否在trie树中
     *
     * @param wordMatchedIndex 已匹配上词前缀的index
     * @return 若存在，则为true
     */
    public boolean isWordMatched(int wordMatchedIndex) {
        if (wordMatchedIndex <= 0) {
            return false;
        }
        int base = entries.get(wordMatchedIndex).base;
        return base < entries.size() && entries.get(base).check == wordMatchedIndex;
    }

    /**
     * 前缀是否在trie树中
     *
     * @param prefix 前缀
     * @return 若存在，则为true
     */
    public boolean isPrefixMatched(String prefix) {
        return match(prefix) < 0;
    }

    /**
     * 匹配字符串.
     *
     * @param str 字符串
     * @return 若匹配上，则为转移后index的负值；否则，则返回已匹配上的字符数
     */
    protected int match(String str) {
        return match(0, str);
    }

    /**
     * 匹配字符串.
     *
     * @param startIndex DAT的开始index
     * @param str        字符串
     * @return 若匹配上，则为转移后index的负值；否则，则返回已匹配上的字符数
     */
    public int match(int startIndex, String str) {
        int index = startIndex;
        for (int i = 0; i < str.length(); i++) {
            index = transition(index, str.charAt(i));
            if (index == MATCH_FAILURE_INDEX) {
                return i;
            }
        }
        return -index;
    }
}
