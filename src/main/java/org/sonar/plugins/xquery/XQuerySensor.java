/*
 * © 2014 by Intellectual Reserve, Inc. All rights reserved.
 */

package org.sonar.plugins.xquery;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.xquery.api.XQueryConstants;
import org.sonar.plugins.xquery.checks.XQueryChecks;
import org.sonar.plugins.xquery.language.Issue;
import org.sonar.plugins.xquery.language.SourceCode;
import org.sonar.plugins.xquery.language.XQueryLineCountParser;
import org.sonar.plugins.xquery.language.XQuerySourceCode;
import org.sonar.plugins.xquery.parser.XQueryTree;
import org.sonar.plugins.xquery.parser.node.DependencyMapper;
import org.sonar.plugins.xquery.parser.reporter.ProblemReporter;
import org.sonar.plugins.xquery.parser.visitor.XQueryAstParser;
import org.sonar.plugins.xquery.parser.visitor.XQueryAstVisitor;
import org.sonar.plugins.xquery.rules.CheckClasses;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XQuerySensor implements Sensor {

    private static final Logger logger = Logger.getLogger(XQuerySensor.class.getName());

    private final XQueryChecks checks;
    private final FileLinesContextFactory fileLinesContextFactory;
    private final FileSystem fileSystem;
    private final NoSonarFilter noSonarFilter;
    private Project project;
    private final FilePredicate mainFilePredicate;

/*    public XQuerySensor(RulesProfile profile, ResourcePerspectives perspectives, FileSystem fileSystem) {
        this.perspectives = perspectives;
        this.fileSystem = fileSystem;
        this.mainFilePredicate = fileSystem.predicates().and(
                fileSystem.predicates().hasType(InputFile.Type.MAIN),
                fileSystem.predicates().hasLanguage(XQueryConstants.XQUERY_LANGUAGE_KEY));

        this.annotationCheckFactory = AnnotationCheckFactory.create(profile, CheckClasses.REPOSITORY_KEY, CheckClasses.getChecks());
    }*/

    public XQuerySensor(
            CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, FileSystem fileSystem, NoSonarFilter noSonarFilter) {
        this.checks = XQueryChecks.createJavaScriptCheck(checkFactory)
                .addChecks(CheckClasses.REPOSITORY_KEY, CheckClasses.getChecks());
        this.fileLinesContextFactory = fileLinesContextFactory;
        this.fileSystem = fileSystem;
        this.noSonarFilter = noSonarFilter;
        this.mainFilePredicate = fileSystem.predicates().and(
                fileSystem.predicates().hasType(InputFile.Type.MAIN),
                fileSystem.predicates().hasLanguage(XQueryConstants.XQUERY_LANGUAGE_KEY));
        //this(checkFactory, fileLinesContextFactory, fileSystem, noSonarFilter, null, null);
    }

   // @Override
    public void analyse(SensorContext context) {
        //Collection<XQueryAstVisitor> checks = annotationCheckFactory.getChecks();
        // List<XQueryAstVisitor> visitors = new ArrayList(checks);
        List<XQueryAstVisitor> visitors = this.checks.visitorChecks();

                // Create a mapper and add it to the visitors so that it can keep track
        // of global declarations and the local declaration stack
        DependencyMapper mapper = new DependencyMapper();
        visitors.add(mapper);

        // Do the first pass to map all the global dependencies
        logger.info("Scanning all files to map dependencies");
        Iterable<InputFile> inputFiles = fileSystem.inputFiles(mainFilePredicate);
        for(InputFile inputFile : inputFiles){
            try {
                SourceCode sourceCode = new XQuerySourceCode(inputFile);
                logger.fine("Mapping " + inputFile.relativePath());

                XQueryAstParser parser = new XQueryAstParser(sourceCode, Arrays.asList(new XQueryAstVisitor[] { mapper }));
                XQueryTree tree = parser.parse();
                parser.mapDependencies(tree, mapper);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not map the dependencies in the file " + inputFile.absolutePath(), e);
            }
        }

        // Now that the global mappings are done we can change the mode to "local"
        mapper.setMode("local");
                
        // Do the second pass to process the checks and other metrics
        logger.info("Scanning all files and gathering metrics");
        for(InputFile inputFile : inputFiles){
            try {
                SourceCode sourceCode = new XQuerySourceCode(inputFile);
                logger.fine("Analyzing " + inputFile.relativePath());

                XQueryAstParser parser = new XQueryAstParser(sourceCode, visitors);
                ProblemReporter reporter = new ProblemReporter();
                XQueryTree tree = parser.parse(reporter);
                parser.process(tree, mapper, reporter);

                // Count the lines of code
                new XQueryLineCountParser(sourceCode).count();

                // Save all the collected metrics
                saveMetrics(inputFile, context, sourceCode);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not analyze the file " + inputFile.absolutePath(), e);
            }
        }
    }

    private void saveMetrics(InputFile file, SensorContext context, SourceCode sourceCode) {
        for (Issue issue : sourceCode.getIssues()) {
            logger.finer("Saving issue: " + issue);
            saveLineIssue(context, file, issue.rule(), issue);
        }
        for (SourceCode.Measure measure : sourceCode.getMeasures()) {
            logger.info("Saving measure: " + measure);
            NewMeasure newMeasure = context.newMeasure();
            newMeasure.on(file).forMetric(measure.getMetric()).withValue(measure.getValue()).save();
        }
    }

    private void saveLineIssue(SensorContext sensorContext, InputFile inputFile, RuleKey ruleKey, Issue issue) {
        NewIssue newIssue = sensorContext.newIssue();

        NewIssueLocation primaryLocation = newIssue.newLocation()
                .message(issue.message())
                .on(inputFile)
                .at(inputFile.selectLine(issue.line()));
        saveIssue(newIssue, primaryLocation, ruleKey, issue);
    }

    private static void saveIssue(NewIssue newIssue, NewIssueLocation primaryLocation, RuleKey ruleKey, Issue issue) {
        newIssue
                .forRule(ruleKey)
                .at(primaryLocation);

//        if (issue.cost() != null) {
//            newIssue.gap(issue.cost());
//        }

        newIssue.save();
    }


    private Iterable<File> getProjectMainFiles() {
        return fileSystem.files(fileSystem.predicates().hasLanguage(XQueryConstants.XQUERY_LANGUAGE_KEY));
    }

    public boolean shouldExecuteOnProject(Project project) {
        return fileSystem.files(fileSystem.predicates().hasLanguage(XQueryConstants.XQUERY_LANGUAGE_KEY)).iterator().hasNext();
    }

    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor
                .onlyOnLanguage(XQueryConstants.XQUERY_LANGUAGE_KEY)
                .name("SonarXQuery")
                .onlyOnFileType(InputFile.Type.MAIN);
    }

    public void execute(SensorContext sensorContext) {
        this.analyse(sensorContext);
    }
}
