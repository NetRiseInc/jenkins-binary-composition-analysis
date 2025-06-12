package io.jenkins.plugins.netrise.asset.uploader.env;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Helper to find and replace Environmental Variables in the text string
* */
public class EnvMapper {

    private static final Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}", Pattern.MULTILINE);

    /**
     * Replace all the occurrences of environmental variables in format ${ENV_VAR}
     * where ENV_VAR is the name of environmental variable.
     *
     * @param sentence The text string that should be replaced with environmental variable values.
     * @param env The map of the environmental variables.
     *
     * @return Processed text.
     * */
    public static String replaceEnv(String sentence, Map<String, String> env) {
        if (sentence == null) {
            throw new IllegalArgumentException("'sentence' should be defined.");
        }

        if (env == null) {
            env = Collections.emptyMap();
        }

        Matcher matcher = pattern.matcher(sentence);
        StringBuilder sb = new StringBuilder();

        int end = 0;
        while (matcher.find()) {
            if (matcher.group() != null && matcher.group(1) != null) {
                String val = env.get( matcher.group(1) );
                sb.append(sentence, end, matcher.start())
                        .append(val != null ? val : "");
                end = matcher.end();
            }
        }
        sb.append(sentence, end, sentence.length());

        return sb.toString();
    }

}
