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
import java.io.FileNotFoundException;
import java.io.IOException;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestFreemarkerComponentry {

  private static final File buildDirectory = new File(System.getProperty("project.build.directory", "target"));
  
  private File nonExistentDirectory;

  private File existingDirectory;
  
  public TestFreemarkerComponentry() {
    super();
  }

  @Before
  public void setUp() {
    assertTrue(buildDirectory.isDirectory());
    assertTrue(buildDirectory.canWrite());
    
    this.nonExistentDirectory = new File("crap");
    assertFalse(this.nonExistentDirectory.exists());

    final File makeMe = new File(buildDirectory, this.getClass().getSimpleName());
    makeMe.deleteOnExit();
    assertFalse(makeMe.exists());
    assertTrue(makeMe.mkdirs());
    assertTrue(makeMe.isDirectory());
    assertTrue(makeMe.canRead());
    assertTrue(makeMe.canWrite());
    this.existingDirectory = makeMe;

    
  }

  @After
  public void tearDown() {
    if (this.existingDirectory != null) {
      this.existingDirectory.delete();
    }
  }
  
  @Test(expected = FileNotFoundException.class)
  public void testFileTemplateLoaderConstructionWithNonexistingDirectory() throws IOException {
    final TemplateLoader templateLoader = new FileTemplateLoader(this.nonExistentDirectory);
  }

  @Test
  public void testFileTemplateLoaderConstructionWithExistingDirectory() throws IOException {
    final TemplateLoader templateLoader = new FileTemplateLoader(this.existingDirectory);
  }

  @Test(expected = FileNotFoundException.class)
  public void testMultiTemplateLoaderConstructionWithNonexistentDirectory() throws IOException {
    final TemplateLoader templateLoader =
      new MultiTemplateLoader(new TemplateLoader[] { new FileTemplateLoader(this.nonExistentDirectory) });
  }

}
