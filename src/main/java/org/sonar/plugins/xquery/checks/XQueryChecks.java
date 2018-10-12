/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.xquery.checks;

import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.xquery.XQuerySensor;
import org.sonar.plugins.xquery.parser.visitor.XQueryAstVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around Checks Object to ease the manipulation of the different JavaScript rule repositories.
 */
public class XQueryChecks {

    private static final Logger LOG = Loggers.get(XQuerySensor.class);

    private final CheckFactory checkFactory;
    private Set<Checks<AbstractCheck>> checksByRepository = new HashSet<>();
    private XQueryChecks(CheckFactory checkFactory) {
        this.checkFactory = checkFactory;
    }

    public static XQueryChecks createJavaScriptCheck(CheckFactory checkFactory) {
        return new XQueryChecks(checkFactory);
    }

    public XQueryChecks addChecks(String repositoryKey, Iterable<Class> checkClass) {
        checksByRepository.add(checkFactory
                .<AbstractCheck>create(repositoryKey)
                .addAnnotatedChecks(checkClass));

        return this;
    }

//    public XQueryChecks addCustomChecks(@Nullable CustomJavaScriptRulesDefinition[] customRulesDefinitions,
//                                            @Nullable CustomRuleRepository[] customRuleRepositories) {
//        if (customRulesDefinitions != null) {
//            LOG.warn("CustomJavaScriptRulesDefinition usage is deprecated. Use CustomRuleRepository API to define custom rules");
//            for (CustomJavaScriptRulesDefinition rulesDefinition : customRulesDefinitions) {
//                addChecks(rulesDefinition.repositoryKey(), ImmutableList.copyOf(rulesDefinition.checkClasses()));
//            }
//        }
//
//        if (customRuleRepositories != null) {
//            for (CustomRuleRepository repo : customRuleRepositories) {
//                addChecks(repo.repositoryKey(), repo.checkClasses());
//            }
//        }
//
//        return this;
//    }

    private List<AbstractCheck> all() {
        List<AbstractCheck> allVisitors = new ArrayList<>();

        for (Checks<AbstractCheck> checks : checksByRepository) {
            allVisitors.addAll(checks.all());
        }

        return allVisitors;
    }

    public List<XQueryAstVisitor> visitorChecks() {
        List<XQueryAstVisitor> checks = new ArrayList<>();
        for (AbstractCheck check : all()) {
            if (check instanceof XQueryAstVisitor) {
                checks.add((XQueryAstVisitor) check);
            }
        }

        return checks;
    }

    public RuleKey ruleKeyFor(AbstractCheck check) {
        RuleKey ruleKey;

        for (Checks<AbstractCheck> checks : checksByRepository) {
            ruleKey = checks.ruleKey(check);

            if (ruleKey != null) {
                return ruleKey;
            }
        }
        return null;
    }

}