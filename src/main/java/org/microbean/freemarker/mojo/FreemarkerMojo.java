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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

import org.microbean.freemarker.LoadableClassesTemplateHashModel;
import org.microbean.freemarker.FlexibleObjectWrapper;
import org.microbean.freemarker.ClassModelFactory;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;

import freemarker.ext.beans.BeansWrapper;

import freemarker.ext.util.ModelFactory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

import org.apache.maven.execution.MavenSession;

import org.apache.maven.model.Build;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugin.logging.Log;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.project.MavenProject;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.IndexReader;

/**
 * Generates a document from a <a
 * href="http://freemarker.org/">Freemarker</a> template.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 */
@Mojo(name = "freemarker")
public class FreemarkerMojo extends AbstractMojo {


  /*
   * Static fields.
   */

  
  /**
   * A {@link Version} representing the version of <a
   * href="http://freemarker.org/">Freemarker</a> in use.
   *
   * <p>This field is never {@code null}.</p>
   */
  static final Version FREEMARKER_VERSION = new Version(Configuration.class.getPackage().getImplementationVersion());


  /*
   * Instance fields.
   */


  /**
   * Whether to skip execution.
   */
  @Parameter(defaultValue = "false", property = "freemarker.skip")
  private boolean skip;  

  /**
   * A {@link Map} of <a
   * href="http://freemarker.org/docs/api/freemarker/ext/util/ModelFactory.html">{@code
   * ModelFactory}</a> instances indexed by the names of classes for
   * which they are suitable.
   */
  @Parameter
  private Map<String, ModelFactory> modelFactories;

  /**
   * The {@link MavenProject} in effect.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private final MavenProject project;

  /**
   * The {@link MavenSession} in effect.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  /**
   * The name of the <a href="http://freemarker.org/">Freemarker</a>
   * template to process.  If set, this parameter should be set to a
   * name that may be processed by a <a
   * href="http://freemarker.org/docs/api/freemarker/cache/TemplateLoader.html">{@code
   * TemplateLoader}</a>.  That means among other things that it
   * should not be assumed that the name is a relative or absolute
   * file path.  If not set, the implication is that whatever <a
   * href="http://freemarker.org/docs/api/freemarker/cache/TemplateLoader.html">{@code
   * TemplateLoader}</a> is in effect can load a template or templates
   * without being told explicitly which template to load.
   *
   * @see TemplateLoader
   */
  @Parameter(property = "freemarker.templateName")
  private String templateName;

  /**
   * The <a
   * href="http://freemarker.org/docs/api/freemarker/template/Configuration.html">{@code
   * Configuration}</a> representing the <a
   * href="http://freemarker.org/">Freemarker</a> template engine.
   *
   * @see Configuration
   */
  @Parameter
  private Configuration configuration;

  /**
   * A {@link File} representing where the (optional) <a
   * href="https://github.com/wildfly/jandex"
   * target="_parent">Jandex</a> index exists, providing efficient
   * access to classes and annotations produced by the project of
   * which this plugin execution is a part.
   *
   * @see IndexView
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/jandex.idx", property = "freemarker.jandexIndexFile")
  private File jandexIndexFile;
  
  /**
   * A {@link Map} of {@link String}s to arbitrary {@link Object}s
   * that will be made available in the <a
   * href="http://freemarker.org/">Freemarker</a> template's <a
   * href="http://freemarker.org/docs/pgui_datamodel.html">context</a>.
   *
   * <p>The <a
   * href="http://freemarker.org/docs/pgui_datamodel.html">data
   * model</a> will contain the following "top-level"
   * template-accessible variables by default, unless the
   * user-supplied data model has values indexed under these keys:</p>
   *
   * <dl>
   *
   * <dt>{@code enums}</dt>
   *
   * <dd>An object <a
   * href="http://freemarker.org/docs/pgui_misc_beanwrapper.html#jdk_15_enums">providing
   * access to {@code enum}s</a>, indexed by classname.</dd>
   *
   * <dt>{@code statics}</dt>
   *
   * <dd>An object <a
   * href="http://freemarker.org/docs/pgui_misc_beanwrapper.html#autoid_60">providing
   * access to static methods and fields</a>, indexed by
   * classname.</dd>
   *
   * <dt>{@code classIndex}</dt>
   *
   * <dd>A Jandex {@link IndexView} <a
   * href="http://wildfly.github.io/jandex/org/jboss/jandex/IndexView.html">providing
   * access to all known classes and annotations</a>; see the {@link
   * #jandexIndexFile} parameter documentation for details.</dd>
   *
   * <dt>{@code project}</dt>
   *
   * <dd>The <a
   * href="https://maven.apache.org/ref/3.3.9/apidocs/org/apache/maven/project/MavenProject.html">{@code
   * MavenProject} instance</a> in effect during this goal's
   * execution.</dd>
   *
   * <dt>{@code classes}</dt>
   *
   * <dd>A load-on-demand map of {@link Class} instances loadable from
   * the context classloader.</dd>
   *
   * <dt>{@code contextClassLoader}</dt>
   *
   * <dd>The context classloader in effect during this goal's
   * execution.</dd>
   *
   * </dl>
   *
   * @see <a
   * href="http://freemarker.org/docs/pgui_misc_beanwrapper.html#autoid_60">the
   * Freemarker manual section concerning {@code static} methods and
   * fields</a>
   *
   * @see <a href="http://freemarker.org/docs/pgui_datamodel.html">the
   * Freemarker manual section concerning the data model</a>
   *
   * @see <a href="https://github.com/wildfly/jandex">the Jandex
   * project</a>
   */
  @Parameter
  private Map<String, Object> dataModel;

  /**
   * The character encoding to use when reading <a
   * href="http://freemarker.org/">Freemarker</a> templates.
   */
  @Parameter(defaultValue = "${project.build.outputEncoding}")
  private String outputEncoding;

  /**
   * The full path to the file that results from processing the
   * template given by the {@link #templateName} parameter, or, if the
   * {@link #templateName} parameter is omitted, the full path to the
   * directory that will contain general template processing output.
   * If the {@link #templateName} parameter is supplied, but this
   * parameter is omitted, a path consisting of
   * <code>${project.build.directory}/</code> concatenated with the
   * value of the {@code templateName} parameter minus its {@code
   * .ftl} suffix, if any, will be used for this parameter instead.
   * If both the {@link #templateName} parameter and this parameter
   * are omitted, then the behavior will be as though the {@link
   * #templateName} parameter were omitted and
   * <code>${project.build.directory}</code> were supplied for this
   * parameter.
   */
  @Parameter(property = "freemarker.outputFile")
  private File outputFile;


  /*
   * Constructors.
   */
  

  /**
   * Creates a new {@link FreemarkerMojo}.
   *
   * @see #FreemarkerMojo(MavenProject, MavenSession)
   */
  public FreemarkerMojo() {
    this(null, null);
  }

  /**
   * Creates a new {@link FreemarkerMojo}.
   *
   * @param project the {@link MavenProject} in effect; may be {@code
   * null}
   *
   * @see #FreemarkerMojo(MavenProject, MavenSession)
   */
  public FreemarkerMojo(final MavenProject project) {
    this(project, null);
  }

  /**
   * Creates a new {@link FreemarkerMojo}.
   *
   * @param project the {@link MavenProject} in effect; may be {@code
   * null}
   *
   * @param session the {@link MavenSession} in effect; may be {@code
   * null}
   *
   * @see #getProject()
   *
   * @see #getSession()
   */
  public FreemarkerMojo(final MavenProject project, final MavenSession session) {
    super();
    this.project = project;
    this.session = session;
  }


  /*
   * Instance methods.
   */
  

  /**
   * Executes the {@code freemarker} goal.
   *
   * @exception MojoExecutionException if a fatal error occured
   */
  @Override
  public void execute() throws MojoExecutionException {
    final Log log = this.getLog();
    
    if (this.isSkip()) {
      if (log != null && log.isInfoEnabled()) {
        log.info("Skipping execution by request.");
      }
      return;
    }
    
    //
    // Determine ModelFactory instances in use.
    //
    
    Map<String, ModelFactory> modelFactoriesByClassName = this.getModelFactories();
    if (modelFactoriesByClassName == null) {
      modelFactoriesByClassName = new HashMap<>();
    }
    modelFactoriesByClassName.putIfAbsent("java.lang.Class", new ClassModelFactory());
    
    Map<? extends Class<?>, ? extends ModelFactory> modelFactories = null;
    try {
      modelFactories = convert(modelFactoriesByClassName);
    } catch (final ClassNotFoundException classNotFoundException) {
      throw new MojoExecutionException(classNotFoundException.getMessage(), classNotFoundException);
    }
    assert modelFactories != null;

    if (log != null && log.isDebugEnabled()) {
      log.debug("Using modelFactories: " + modelFactories);
    }

    //
    // Determine Configuration in use.
    //
    
    Configuration configuration = this.getConfiguration();
    if (configuration == null) {
      configuration = new Configuration(FREEMARKER_VERSION);
      configuration.setDefaultEncoding("UTF-8");
      configuration.setAPIBuiltinEnabled(true);
    }
    configuration.setIncompatibleImprovements(FREEMARKER_VERSION);
    final DefaultObjectWrapper beansWrapper = new FlexibleObjectWrapper(FREEMARKER_VERSION, modelFactories);
    beansWrapper.setExposureLevel(BeansWrapper.EXPOSE_ALL); // http://freemarker.org/docs/api/freemarker/ext/beans/BeansWrapper.html#setExposureLevel-int-
    beansWrapper.setForceLegacyNonListCollections(false); // http://freemarker.org/docs/api/freemarker/template/DefaultObjectWrapper.html#setForceLegacyNonListCollections-boolean-
    beansWrapper.setUseAdaptersForContainers(true); // http://freemarker.org/docs/api/freemarker/template/DefaultObjectWrapper.html#setUseAdaptersForContainers-boolean-
    configuration.setObjectWrapper(beansWrapper);
    assert beansWrapper == configuration.getObjectWrapper();
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    configuration.setLogTemplateExceptions(false);
    if (log != null && log.isDebugEnabled()) {
      log.debug("Using configuration: " + configuration);
    }
    
    //
    // Determine what template names to use.  There may be just one,
    // and there must be at least one.
    //
    
    final Set<String> templateNames = this.getTemplateNames(configuration);
    if (templateNames == null || templateNames.isEmpty()) {
      throw new MojoExecutionException("No templates to process");
    } else if (templateNames.size() > 1) {
      // If there are lots of templates to process, then treat
      // outputFile as a directory, not a regular file.
      final File outputFile = this.getOutputFile();
      if (outputFile != null) {
        if (outputFile.exists() && !outputFile.isDirectory()) {
          throw new MojoExecutionException("outputFile was an existing non-directory: " + outputFile);
        } else {
          outputFile.mkdirs();
        }
      }
    }
    if (log != null && log.isDebugEnabled()) {
      log.debug("Using templateNames: " + templateNames);
    }


    //
    // Find the Jandex index if we can get it.
    //
    
    IndexView indexView = null;
    File jandexIndexFile = this.getJandexIndexFile();
    if (jandexIndexFile != null && jandexIndexFile.isFile() && jandexIndexFile.canRead()) {
      IndexReader reader = null;
      try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(jandexIndexFile))) {
        indexView = new IndexReader(inputStream).read();
      } catch (final IOException ioException) {
        throw new MojoExecutionException(ioException.getMessage(), ioException);
      }
      assert indexView != null;
    } else {
      // TODO: index on the fly
    }
    if (log != null && log.isDebugEnabled()) {
      log.debug("Using Jandex index file: " + jandexIndexFile);
    }

    //
    // Set up the data model for the Freemarker template engine.
    //
    
    Map<String, Object> dataModel = this.getDataModel();
    if (dataModel == null) {
      dataModel = new HashMap<>();
    }
    dataModel.put("enums", beansWrapper.getEnumModels()); // note: not putIfAbsent
    dataModel.put("statics", beansWrapper.getStaticModels()); // note: not putIfAbsent
    dataModel.putIfAbsent("classIndex", indexView);
    dataModel.putIfAbsent("project", this.getProject());
    dataModel.putIfAbsent("session", this.getSession());
    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    dataModel.putIfAbsent("classes", new LoadableClassesTemplateHashModel(beansWrapper, contextClassLoader));
    dataModel.putIfAbsent("contextClassLoader", contextClassLoader);
    if (log != null && log.isDebugEnabled()) {
      log.debug("Using dataModel: " + dataModel);
    }

    //
    // Determine what output encoding to use.
    //

    String outputEncoding = this.getOutputEncoding();
    if (outputEncoding == null) {
      outputEncoding = "UTF-8";
    }
    if (log != null && log.isDebugEnabled()) {
      log.debug("Using outputEncoding: " + outputEncoding);
    }
    
    //
    // Process templates.
    //
    
    for (final String templateName : templateNames) {
      if (templateName == null) {
        throw new MojoExecutionException("Encountered null template name in templateNames; check the getTemplateNames() method");
      }

      final File outputFile = this.getOutputFile(templateName);
      if (outputFile == null) {
        if (log != null && log.isDebugEnabled()) {
          log.debug("No suitable outputFile found for a template with the name " + templateName + "; skipping processing");
        }   
      } else {
        if (log != null && log.isDebugEnabled()) {
          log.debug("Loading template: " + templateName);
          log.debug("Output file: " + outputFile);
        }
        Template template = null;
        try {
          template = configuration.getTemplate(templateName);
        } catch (final IOException exception) {
          throw new MojoExecutionException(exception.getMessage(), exception);
        }
        if (log != null && log.isDebugEnabled()) {
          log.debug("Compiled template: " + template);
          log.debug("Processing...");
        }        
        try (final Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), outputEncoding))) {
          template.process(dataModel, out);
          if (log != null && log.isDebugEnabled()) {
            log.debug("...processing complete.");
          }
        } catch (final IOException | TemplateException exception) {
          throw new MojoExecutionException(exception.getMessage(), exception);
        }
      }
    }

    if (log != null && log.isDebugEnabled()) {
      log.debug("All template processing complete.");
    }
  }

  /**
   * Returns a {@link File} representing the path where the output of
   * processing a Freemarker template with the supplied {@code
   * templateName} should be deposited.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * @param templateName the name of a Freemarker template; must not
   * be {@code null}
   *
   * @return a non-{@code null} {@link File} representing the path
   * where the output of processing a Freemarker template with the
   * supplied {@code templateName} should be deposited
   *
   * @exception NullPointerException if {@code templateName} is {@code
   * null}
   *
   * @see #getDefaultOutputFile(String)
   *
   * @see #getOutputFile()
   */
  protected File getOutputFile(final String templateName) {
    Objects.requireNonNull(templateName);
    File returnValue = null;
    if (templateName.equals(this.getTemplateName())) {
      // If we are being asked for the outputFile for the
      // user-specified templateName, then use the user-specified
      // outputFile if there is one.
      returnValue = this.getOutputFile();
    } else {
      File outputFile = this.getOutputFile();
      if (outputFile == null) {
        outputFile = this.getProjectBuildDirectory();
      }
      assert outputFile != null;
      returnValue = new File(outputFile, stripFtlSuffix(templateName));
    }
    if (returnValue == null) {
      returnValue = this.getDefaultOutputFile(templateName);
    }
    assert returnValue != null;
    final File directory = returnValue.getParentFile();
    assert directory != null;
    directory.mkdirs();
    return returnValue;
  }

  /**
   * Returns a {@link File} representing the <em>default location</em> for the
   * output from the processing of a Freemarker template with the
   * supplied {@code templateName}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * <p>This implementation returns a new {@link File} formed from a
   * concatenation of the {@linkplain #getProjectBuildDirectory()
   * default Maven project build directory} and the supplied {@code
   * templateName}, minus its {@code .ftl} suffix (if it has one).</p>
   *
   * @param templateName the name of the template for which an output
   * file should be returned; must not be {@code null}
   *
   * @return a non-{@code null} {@link File} representing the
   * <em>default location</em> for the output from the processing of a
   * Freemarker template with the supplied {@code templateName}
   *
   * @exception NullPointerException if {@code templateName} is {@code
   * null}
   *
   * @see #getProjectBuildDirectory()
   *
   * @see #getOutputFile(String)
   */
  protected File getDefaultOutputFile(final String templateName) {
    Objects.requireNonNull(templateName);
    return new File(this.getProjectBuildDirectory(), stripFtlSuffix(templateName));
  }

  /**
   * Removes any trailing "{@code .ftl}" from the supplied {@link
   * String} and returns the result.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @param templateName the name of the template whose optional
   * {@code .ftl} suffix will be stripped; may be {@code null} in
   * which case {@code null} will be returned
   *
   * @return the doctored template name, or {@code null}
   */
  private static final String stripFtlSuffix(final String templateName) {
    String returnValue = templateName;
    if (templateName != null) {
      final int length = templateName.length();
      // 4 == ".ftl".length()
      if (length > 4 && templateName.toLowerCase().endsWith(".ftl")) {
        returnValue = templateName.substring(0, length - 4);
      }
    }
    return returnValue;
  }
  
  /**
   * A convenience method that returns a {@link File} representing the
   * {@linkplain Build#getDirectory() default Maven build directory}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>The {@link File} returned by this method is guaranteed to
   * {@linkplain File#isDirectory() be a directory}.</p>
   *
   * @return a non-{@code null} {@linkplain File#isDirectory()
   * <code>File</code> representing a directory} that is the
   * {@linkplain Build#getDirectory() default Maven build directory}
   *
   * @see Build#getDirectory()
   */
  protected final File getProjectBuildDirectory() {
    final MavenProject project = this.getProject();
    if (project == null) {
      throw new IllegalStateException("project cannot be null");
    }
    final Build projectBuild = project.getBuild();
    if (projectBuild == null) {
      throw new IllegalStateException("project.getBuild() cannot return null");
    }
    final String projectBuildDirectory = projectBuild.getDirectory();
    if (projectBuildDirectory == null) {
      throw new IllegalStateException("project.getBuild().getDirectory() cannot return null");
    }
    final File projectBuildDirectoryFile = new File(projectBuildDirectory);
    assert projectBuildDirectoryFile.isDirectory();
    return projectBuildDirectoryFile;
  }

  /**
   * Returns {@code true} if an invocation of the {@link #execute()}
   * method should do nothing.
   *
   * @return {@code true} if an invocation of the {@link #execute()}
   * method should do nothing; {@code false} otherwise
   *
   * @see #setSkip(boolean)
   *
   * @see #execute()
   */
  public boolean isSkip() {
    return this.skip;
  }

  /**
   * Sets whether an invocation of the {@link #execute()}
   * method should do nothing.
   *
   * @param skip if {@code true}, an invocation of the {@link #execute()}
   * method should do nothing
   *
   * @see #isSkip()
   *
   * @see #execute()
   */
  public void setSkip(final boolean skip) {
    this.skip = skip;
  }

  /**
   * Returns the {@link MavenProject} in effect.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return the {@link MavenProject} in effect, or {@code null}
   *
   * @see #FreemarkerMojo(MavenProject)
   */
  public final MavenProject getProject() {
    return this.project;
  }

  /**
   * Returns the {@link MavenSession} in effect.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return the {@link MavenSession} in effect, or {@code null}
   *
   * @see #FreemarkerMojo(MavenProject, MavenSession)
   */
  public final MavenSession getSession() {
    return this.session;
  }

  /**
   * Returns the {@link Configuration} that represents the Freemarker
   * template engine.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link Configuration}, or {@code null}
   *
   * @see #setConfiguration(Configuration)
   */
  public Configuration getConfiguration() {
    return this.configuration;
  }

  /**
   * Installs the {@link Configuration} that represents the Freemarker
   * template engine.
   *
   * @param configuration the {@link Configuration} to use; may be
   * {@code null}
   *
   * @see #getConfiguration()
   */
  public void setConfiguration(final Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Returns a {@link File} representing the path to a <a
   * href="http://wildfly.github.io/jandex/org/jboss/jandex/package-summary.html#package.description"
   * target="_parent">Jandex</a> index file.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link File} representing the path to a <a
   * href="http://wildfly.github.io/jandex/org/jboss/jandex/package-summary.html#package.description"
   * target="_parent">Jandex</a> index file, or {@code null}
   *
   * @see #setJandexIndexFile(File)
   */
  public File getJandexIndexFile() {
    return this.jandexIndexFile;
  }

  /**
   * Installs a {@link File} representing the path to a <a
   * href="http://wildfly.github.io/jandex/org/jboss/jandex/package-summary.html#package.description"
   * target="_parent">Jandex</a> index file.
   *
   * @param jandexIndexFile the {@link File} to use; may be {@code
   * null}
   *
   * @see #getJandexIndexFile()
   *
   * @see <a
   * href="http://wildfly.github.io/jandex/org/jboss/jandex/package-summary.html#package.description"
   * target="_parent">Jandex</a>
   */
  public void setJandexIndexFile(final File jandexIndexFile) {
    this.jandexIndexFile = jandexIndexFile;
  }
  
  /**
   * Returns a {@link File} representing the full path to the file
   * that will result from processing the template given by the {@link
   * #getTemplateName() templateName} parameter.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link File} representing the full path to the file
   * that will result from processing the template given by the {@link
   * #getTemplateName() templateName} parameter, or {@code null}
   *
   * @see #setOutputFile(File)
   */
  public File getOutputFile() {
    return this.outputFile;
  }

  /**
   * Sets the {@link File} representing the full path to the file that
   * will result from processing the template given by the {@link
   * #getTemplateName() templateName} parameter.
   *
   * @param outputFile the {@link File} representing the full path to
   * the file that will result from processing the template given by
   * the {@link #getTemplateName() templateName} parameter; may be
   * {@code null}
   *
   * @see #getOutputFile()
   */
  public void setOutputFile(final File outputFile) {
    this.outputFile = outputFile;
  }

  /**
   * Returns the character encoding scheme used when writing the
   * {@linkplain #getOutputFile() output <code>File</code>}.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return the character encoding scheme used when writing the
   * {@linkplain #getOutputFile() output <code>File</code>}, or {@code
   * null}
   *
   * @see #setOutputEncoding(String)
   */
  public String getOutputEncoding() {
    return this.outputEncoding;
  }

  /**
   * Installs the character encoding scheme used when writing the
   * {@linkplain #getOutputFile() output <code>File</code>}.
   *
   * @param outputEncoding the new scheme; may be {@code null}
   *
   * @see #getOutputEncoding()
   */
  public void setOutputEncoding(final String outputEncoding) {
    this.outputEncoding = outputEncoding;
  }

  /**
   * Returns the name of the Freemarker template to process.
   *
   * <p>This method may return {@code null}.</p>
   *
   * <p>Overrides of this method are permitted to return {@code
   * null}.</p>
   *
   * @return the name of the Freemarker template to process, or {@code
   * null}
   *
   * @see #setTemplateName(String)
   */
  public String getTemplateName() {
    return this.templateName;
  }

  /**
   * Returns a {@link Set} of names of Freemarker templates to
   * process.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must not return {@code null}.</p>
   *
   * @param configuration a {@link Configuration} that might be
   * inspected for its {@link Configuration#getTemplateLoader()
   * TemplateLoader}; may be {@code null}
   *
   * @return a non-{@code null}, {@linkplain
   * Collections#unmodifiableSet(Set) unmodifiable <code>Set</code>}
   * of template names
   *
   * @see #getTemplateName()
   */
  protected Set<String> getTemplateNames(final Configuration configuration) {
    final Set<String> templateNames = new LinkedHashSet<>();
    final String templateName = this.getTemplateName();
    if (templateName == null) {
      if (configuration != null) {
        final TemplateLoader templateLoader = configuration.getTemplateLoader();
        if (templateLoader instanceof FileTemplateLoader) {
          final File templateLoaderDirectory = ((FileTemplateLoader)templateLoader).getBaseDirectory();
          if (templateLoaderDirectory != null && templateLoaderDirectory.isDirectory()) {
            final String[] ftlFilenames = templateLoaderDirectory.list((d, n) -> n != null && n.toLowerCase().endsWith(".ftl"));
            if (ftlFilenames != null && ftlFilenames.length > 0) {
              for (final String ftlFilename : ftlFilenames) {
                if (ftlFilename != null) {
                  templateNames.add(ftlFilename);
                }
              }
            }
          }
        }
      }
    } else {
      templateNames.add(templateName);
    }
    if (templateNames == null || templateNames.isEmpty()) {
      return Collections.emptySet();
    } else {
      return Collections.unmodifiableSet(templateNames);
    }
  }

  /**
   * Sets the name of the Freemarker template to process.
   *
   * <p>Note that the interpretation of this name is the domain solely
   * of the {@link TemplateLoader} {@linkplain
   * Configuration#setTemplateLoader(TemplateLoader) installed} as
   * part of the {@link Configuration} defining the Freemarker
   * environment.</p>
   *
   * @param templateName the name of the Freemarker template to
   * process; may be {@code null}
   *
   * @see #getTemplateName()
   *
   * @see Configuration#setTemplateLoader(TemplateLoader)
   *
   * @see TemplateLoader
   */
  public void setTemplateName(final String templateName) {
    this.templateName = templateName;
  }

  /**
   * Returns a {@link Map} of the {@link ModelFactory} instances
   * {@linkplain BeansWrapper#getModelFactory(Class) to be used} by a
   * {@link BeansWrapper} indexed by the names of the {@link Class}es
   * to which they apply.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a {@link Map} of the {@link ModelFactory} instances
   * {@linkplain BeansWrapper#getModelFactory(Class) to be used} by a
   * {@link BeansWrapper} indexed by the names of the {@link Class}es
   * to which they apply, or {@code null}
   *
   * @see #setModelFactories(Map)
   */
  public Map<String, ModelFactory> getModelFactories() {
    return this.modelFactories;
  }

  /**
   * Installs a {@link Map} of the {@link ModelFactory} instances
   * {@linkplain BeansWrapper#getModelFactory(Class) to be used} by a
   * {@link BeansWrapper} indexed by the names of the {@link Class}es
   * to which they apply.
   *
   * @param modelFactories the {@link Map} to use; may be {@code null}
   *
   * @see #getModelFactories()
   */
  public void setModelFactories(final Map<String, ModelFactory> modelFactories) {
    this.modelFactories = modelFactories;
  }

  /**
   * Returns a {@link Map} representing the <em>data model</em>
   * Freemarker will use when processing a template.
   *
   * <p>This method may return {@code null}.</p>
   *
   * @return a Freemarker data model, or {@code null}
   *
   * @see #setDataModel(Map)
   */
  public Map<String, Object> getDataModel() {
    return this.dataModel;
  }

  /**
   * Installs a {@link Map} representing the <em>data model</em>
   * Freemarker will use when processing a template.
   *
   * @param dataModel the {@link Map} to use; may be {@code null}
   *
   * @see #getDataModel()
   */
  public void setDataModel(final Map<String, Object> dataModel) {
    this.dataModel = dataModel;
  }
  

  /*
   * Static methods.
   */


  /**
   * Converts a {@link Map} of classnames indexing {@link
   * ModelFactory} instances into a {@link Map} of {@link Class}
   * instances indexing the same {@link ModelFactory} instances by
   * {@linkplain ClassLoader#loadClass(String) loading the
   * <code>Class</code>} corresponding to each classname using the
   * {@link Thread#getContextClassLoader() context classloader} and
   * returns the result.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param originalModelFactoryMap the {@link Map} to convert; may be
   * {@code null} in which case an {@linkplain Map#isEmpty() empty}
   * {@link Map} will be returned
   *
   * @return a non-{@code null} {@link Map} of {@link Class} instances
   * indexing appropriate {@link ModelFactory} instances
   *
   * @exception ClassNotFoundException if a given {@link Class} could
   * not be {@linkplain ClassLoader#loadClass(String) loaded}
   *
   * @see ClassLoader#loadClass(String)
   *
   * @see Thread#getContextClassLoader()
   */
  private static final Map<? extends Class<?>, ? extends ModelFactory> convert(final Map<? extends String, ? extends ModelFactory> originalModelFactoryMap) throws ClassNotFoundException {
    final Map<Class<?>, ModelFactory> returnValue = new HashMap<>();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    assert cl != null;
    if (originalModelFactoryMap != null && !originalModelFactoryMap.isEmpty()) {
      final Iterable<? extends Entry<? extends String, ? extends ModelFactory>> entries = originalModelFactoryMap.entrySet();
      if (entries != null) {
        for (final Entry<? extends String, ? extends ModelFactory> entry : entries) {
          if (entry != null) {
            final String className = entry.getKey();
            if (className == null) {
              returnValue.put(null, entry.getValue());
            } else {
              returnValue.put(cl.loadClass(className), entry.getValue());
            }
          }
        }
      }
    }
    return returnValue;
  }
  
}
