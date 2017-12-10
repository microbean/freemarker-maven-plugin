/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.freemarker.mojo;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.Reader;

import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.testing.MojoRule;

import org.apache.maven.plugin.testing.resources.TestResources;

import org.apache.maven.project.MavenProject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestFreemarkerMojo {

  private static final File projectBuildDirectory = new File(System.getProperty("project.build.directory", "target"));

  static {
    assertTrue(projectBuildDirectory.isDirectory());
  }
  
  // See https://vzurczak.wordpress.com/2014/07/23/write-unit-tests-for-a-maven-plug-in/
  
  @Rule
  public MojoRule mojoRule = new MojoRule();

  @Rule
  public TestResources testResources = new TestResources();

  private File testResourcesBaseDirectory;

  private File testProjectTargetDirectory;

  private FreemarkerMojo mojo;
  
  public TestFreemarkerMojo() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    this.testResourcesBaseDirectory = this.testResources.getBasedir("valid");
    assertNotNull(this.testResourcesBaseDirectory);
    assertTrue(this.testResourcesBaseDirectory.isDirectory());
    final MavenProject mavenProject = this.mojoRule.readMavenProject(this.testResourcesBaseDirectory);
    assertNotNull(mavenProject);
    this.testProjectTargetDirectory = new File(mavenProject.getBuild().getDirectory());
    this.testProjectTargetDirectory.mkdirs();
    this.mojo = (FreemarkerMojo)this.mojoRule.lookupConfiguredMojo(mavenProject, "freemarker");
    assertNotNull(this.mojo);
    assertNotNull(this.mojo.getConfiguration());
    assertNotNull(this.mojo.getProject());
    assertNotNull(this.mojo.getTemplateName());
  }
  
  @Test
  public void testExecution() throws IOException, MojoExecutionException {
    assertNotNull(this.mojo);
    this.mojo.execute();
    final File templateOutput = new File(this.testProjectTargetDirectory, "test");
    assertTrue(templateOutput.isFile());
    final Properties properties = new Properties();
    try (final Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(templateOutput), "UTF-8"))) {
      properties.load(reader);
    }
    assertEquals(FreemarkerMojo.class.getName(), properties.get("classname"));
    assertEquals(this.testProjectTargetDirectory.getPath(), properties.get("project.build.directory"));
    assertEquals("public void " + FreemarkerMojo.class.getName() + ".execute() throws " + MojoExecutionException.class.getName(),
                 properties.get("executeMethod"));
    assertEquals("test.ftl", properties.get("templateName"));
  }
  
}
