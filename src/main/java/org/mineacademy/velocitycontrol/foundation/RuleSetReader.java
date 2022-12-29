package org.mineacademy.velocitycontrol.foundation;

import com.google.common.base.Preconditions;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.foundation.model.Rule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class RuleSetReader<T extends Rule> {
    private final String newKeyword;

    public RuleSetReader(String newKeyword) {
        this.newKeyword = newKeyword;
    }

    public abstract void load();

    protected final List<T> loadFromFile(String path) {
        File file = FileUtil.extract(path);
        return this.loadFromFile(file);
    }

    private final List<T> loadFromFile(File file) {
        List<T> rules = new ArrayList();
        List<String> lines = FileUtil.readLines(file);
        T rule = null;
        String match = null;

        for(int i = 0; i < lines.size(); ++i) {
            String line = ((String)lines.get(i)).trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if (line.startsWith(this.newKeyword + " ")) {
                    if (rule != null && this.canFinish()) {
                        rules.add(rule);
                    }

                    try {
                        match = line.replace(this.newKeyword + " ", "");
                        rule = this.createRule(file, match);
                    } catch (Throwable var10) {
                        //Common.throwError(var10, new String[]{"Error creating rule from line (" + (i + 1) + "): " + line, "File: " + file, "Error: %error", "Processing aborted."});
                        VelocityControl.getLogger().error("Error creating rule from line (" + (i + 1) + "): " + line, "File: " + file, "Error: %error", "Processing aborted.");
                        var10.printStackTrace();
                        return rules;
                    }
                } else {
                    if (!this.onNoMatchLineParse()) {
                        Preconditions.checkNotNull(match, "Cannot define operator when no rule is being created! File: '" + file + "' Line (" + (i + 1) + "): '" + line + "'");
                    }

                    if (rule != null) {
                        try {
                            rule.onOperatorParse(line.split(" "));
                        } catch (Throwable var9) {
                            //Common.throwError(var9, new String[]{"Error parsing rule operator from line (" + (i + 1) + "): " + line, "File: " + file, "Error: %error"});
                            VelocityControl.getLogger().error("Error parsing rule operator from line (" + (i + 1) + "): " + line, "File: " + file, "Error: %error");
                            var9.printStackTrace();
                        }
                    }
                }
            }

            if (i + 1 == lines.size() && rule != null && this.canFinish()) {
                rules.add(rule);
            }
        }

        return rules;
    }

    protected boolean onNoMatchLineParse() {
        return false;
    }

    protected boolean canFinish() {
        return true;
    }

    protected abstract T createRule(File var1, String var2);
}