/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 AutoScout24 GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.autoscout24.gradle

import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.felixschulze.teamcity.TeamCityStatusMessageHelper
import de.felixschulze.teamcity.TeamCityStatusType
import java.util.regex.Pattern

class TodoTask extends DefaultTask {

    private Pattern todoPattern

    private static final Logger LOG = LoggerFactory.getLogger(TodoTask.class)

    @TaskAction
    def checkTodo() throws IOException {

        int numberOfTodosFound = 0

        String sourceFolder = project.todo.sourceFolder

        new File(sourceFolder).eachFileRecurse { file ->

            String name = file.getName()

            String extension = FilenameUtils.getExtension(name)

            if (extension != null && project.todo.fileExtensions.contains(extension) && !file.isDirectory()) {
                BufferedReader reader = new BufferedReader(new FileReader(file))

                int lineNumber = 1
                String line = reader.readLine()

                while (line != null) {
                    if (getTodoPattern().matcher(line).find()) {
                        numberOfTodosFound++
                        String logString = name + ":" + lineNumber + ": " + line.trim()
                        LOG.warn("WARNING: TODO found: '" + logString + "'")
                        if (project.todo.teamCityLog) {
                            if (project.todo.failIfFound) {
                                println TeamCityStatusMessageHelper.buildStatusString(TeamCityStatusType.FAILURE, logString);
                            } else {
                                println TeamCityStatusMessageHelper.buildStatusString(TeamCityStatusType.WARNING, logString);
                            }
                        }
                    }

                    line = reader.readLine()
                    lineNumber++
                }

                reader.close()

            }
        }

        if (numberOfTodosFound > 0) {
            if (project.todo.failIfFound) {
                throw new GradleScriptException("ERROR: " + numberOfTodosFound + " TODOs found.", null)
            } else {
                LOG.warn("WARNING: " + numberOfTodosFound + " TODOs found.")
            }
        }
    }

    Pattern getTodoPattern() {
        if (todoPattern == null) {
            String pattern = project.todo.todoPattern
            String todoSuffixPattern = project.todo.todoSuffixPattern
            if (todoSuffixPattern) {
                pattern = pattern + todoSuffixPattern
            }
            todoPattern = Pattern.compile(pattern)
        }
        return todoPattern
    }
}
