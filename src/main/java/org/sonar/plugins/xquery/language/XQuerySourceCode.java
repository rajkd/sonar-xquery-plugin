/*
 * © 2014 by Intellectual Reserve, Inc. All rights reserved.
 */

/*
 * (c) 2014 by Intellectual Reserve, Inc. All rights reserved.
 */

package org.sonar.plugins.xquery.language;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XQuerySourceCode implements SourceCode {

    private final InputFile inputFile;
    private List<String> code = new ArrayList<>();
    private final List<Measure> measures = new ArrayList<>();
    private final List<Issue> issues = new ArrayList<>();

    /**
     * Creates a source code object using the string code.
     *
     * @param code a string of source code
     */
    public XQuerySourceCode(String code) {
        this(Arrays.asList(StringUtils.split(code, '\n')));
    }

    /**
     * Creates a source code object using the list of strings for code. Since
     * this is not a file it uses the the code string as
     * the source name.
     *
     * @param code a list of strings code lines
     */
    public XQuerySourceCode(List<String> code) {
        this(code, null);
    }


    public XQuerySourceCode(List<String> code, InputFile inputFile) {
        this.code = code;
        this.inputFile = inputFile;
    }

    public XQuerySourceCode(InputFile inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public String getCodeString() {
        return StringUtils.join(getCode(), "\n");
    }

    @Override
    public List<String> getCode() {
        if (inputFile != null && code.size() == 0) {
            try {
                code = FileUtils.readLines(inputFile.file(), "UTF-8");
                return code;
            } catch (IOException e) {
                throw new SonarException(e);
            }
        } else {
            return code;
        }
    }

    @Override
    public List<Issue> getIssues() {
        return issues;
    }

    @Override
    public List<Measure> getMeasures() {
        return measures;
    }

    @Override
    public void addIssue(Issue issue) {
        this.issues.add(issue);
    }

    @Override
    public void addMeasure(Metric metric, int value) {
        this.measures.add(new Measure(metric, value));
    }

    @Override
    public Measure getMeasure(Metric metric) {
        for (Measure measure : measures) {
            if (measure.getMetric().equals(metric)) {
                return measure;
            }
        }
        return null;
    }

    @Override
    public InputFile getInputFile(){
        return this.inputFile;
    }

}
