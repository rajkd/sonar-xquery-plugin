/*
 * © 2014 by Intellectual Reserve, Inc. All rights reserved.
 */

package org.sonar.plugins.xquery.language;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;

import java.util.List;

public interface SourceCode {

   String getCodeString();

   List<String> getCode();

   List<Issue> getIssues();

   List<Measure> getMeasures();

   void addIssue(Issue issue);

   void addMeasure(Metric metric, int value);

   Measure getMeasure(Metric metric);

   InputFile getInputFile();

    class Measure{
        protected String metricKey;
        protected Metric metric;
        protected Integer value;

        public Measure(Metric metric, Integer value) {
            this.metric = metric;
            this.value = value;
        }

        public String getMetricKey() {
            return metricKey;
        }

        public void setMetricKey(String metricKey) {
            this.metricKey = metricKey;
        }

        public Metric getMetric() {
            return metric;
        }

        public void setMetric(Metric metric) {
            this.metric = metric;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }
}
