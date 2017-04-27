package com.kalessil.phpStorm.phpInspectionsEA.inspectors.semanticalAnalysis.classes;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.Types;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public class MissingIssetImplementationInspector extends BasePhpInspection {
    private static final String messagePattern = "%c% needs to implement __isset to properly work here.";

    @NotNull
    public String getShortName() {
        return "MissingIssetImplementationInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpEmpty(PhpEmpty expression) {
                analyzeDispatchedExpressions(expression.getVariables());
            }

            public void visitPhpIsset(PhpIsset expression) {
                analyzeDispatchedExpressions(expression.getVariables());
            }

            private void analyzeDispatchedExpressions(@NotNull PhpExpression[] parameters) {
                final Project project       = holder.getProject();
                final PhpIndex projectIndex = PhpIndex.getInstance(project);

                for (final PhpExpression parameter : parameters) {
                    final Set<String> resolvedTypes = parameter.getType().global(project).filterUnknown().getTypes();
                    for (final String type : resolvedTypes) {
                        final String normalizedType = Types.getType(type);
                        if (normalizedType.startsWith("\\")) {
                            final Collection<PhpClass> classes = projectIndex.getClassesByFQN(normalizedType);
                            if (!classes.isEmpty()) {
                                final PhpClass clazz = classes.iterator().next();
                                if (null == clazz.findMethodByName("__isset")) {
                                    final String message = messagePattern.replace("%c%", clazz.getFQN());
                                    holder.registerProblem(parameter, message, ProblemHighlightType.GENERIC_ERROR);

                                    break;
                                }
                            }
                        }
                    }
                    resolvedTypes.clear();
                }
            }
        };
    }
}
