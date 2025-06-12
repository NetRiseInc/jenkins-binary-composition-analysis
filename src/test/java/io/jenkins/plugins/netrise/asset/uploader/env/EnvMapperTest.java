package io.jenkins.plugins.netrise.asset.uploader.env;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class EnvMapperTest {

    @Test
    void testReplaceEnv_WithExistingVariables() {
        Map<String, String> env = new HashMap<>();
        env.put("USER", "Alice");
        env.put("HOME", "/home/alice");

        String sentence = "Hello ${USER}, your home directory is ${HOME}.";
        String expected = "Hello Alice, your home directory is /home/alice.";

        assertEquals(expected, EnvMapper.replaceEnv(sentence, env));
    }

    @Test
    void testReplaceEnv_WithMissingVariables() {
        Map<String, String> env = new HashMap<>();
        env.put("USER", "Bob");

        String sentence = "Hello ${USER}, your home directory is ${HOME}.";
        String expected = "Hello Bob, your home directory is ."; // Missing HOME value is replaced with an empty string

        assertEquals(expected, EnvMapper.replaceEnv(sentence, env));
    }

    @Test
    void testReplaceEnv_WithNoVariables() {
        Map<String, String> env = new HashMap<>();

        String sentence = "Static text with no placeholders.";
        String expected = "Static text with no placeholders.";

        assertEquals(expected, EnvMapper.replaceEnv(sentence, env));
    }

    @Test
    void testReplaceEnv_EmptySentence() {
        Map<String, String> env = new HashMap<>();
        env.put("USER", "Alice");

        String sentence = "";
        String expected = "";

        assertEquals(expected, EnvMapper.replaceEnv(sentence, env));
    }

    @Test
    void testReplaceEnv_NullSentence() {
        Map<String, String> env = new HashMap<>();
        env.put("USER", "Alice");

        String sentence = null;

        assertThrowsExactly(IllegalArgumentException.class, () -> EnvMapper.replaceEnv(sentence, env));
    }

    @Test
    void testReplaceEnv_NullEnv() {
        Map<String, String> env = null;

        String sentence = "Static text with no placeholders.";
        String expected = "Static text with no placeholders.";

        assertEquals(expected, EnvMapper.replaceEnv(sentence, env));
    }

}
