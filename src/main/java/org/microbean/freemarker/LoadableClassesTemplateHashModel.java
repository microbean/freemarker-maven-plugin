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
package org.microbean.freemarker;

import java.util.Objects;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.WrappingTemplateModel;

/**
 * A {@link WrappingTemplateModel} and a {@link TemplateHashModel}
 * that makes the universe of {@link Class} instances that are
 * loadable by a given {@link ClassLoader} look like a map.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #get(String)
 */
public class LoadableClassesTemplateHashModel extends WrappingTemplateModel implements TemplateHashModel {


  /*
   * Instance fields.
   */


  /**
   * The {@link ClassLoader} to use to {@linkplain
   * ClassLoader#loadClass(String) load classes}.
   *
   * <p>This field is never {@code null}.</p>
   */
  private final ClassLoader classLoader;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link LoadableClassesTemplateHashModel}.
   *
   * @param objectWrapper an {@link ObjectWrapper} used by the
   * {@linkplain WrappingTemplateModel superclass}; must not be {@code
   * null}
   *
   * @param classLoader the {@link ClassLoader} to use to {@linkplain
   * ClassLoader#loadClass(String) load classes}; must not be {@code
   * null}
   *
   * @exception NullPointerException if {@code classLoader} is {@code
   * null}
   */
  public LoadableClassesTemplateHashModel(final ObjectWrapper objectWrapper, final ClassLoader classLoader) {
    super(objectWrapper);
    Objects.requireNonNull(classLoader);
    this.classLoader = classLoader;
  }


  /*
   * Instance methods.
   */
  

  /**
   * Returns {@code false} when invoked to indicate that there are
   * always {@link Class} instances that can be {@linkplain
   * ClassLoader#loadClass(String) loaded}.
   *
   * @return {@code false} when invoked
   */
  @Override
  public final boolean isEmpty() {
    return false;
  }

  /**
   * Returns a {@link TemplateModel} representing a {@linkplain
   * ClassLoader#loadClass(String) loaded <code>Class</code>}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param className the name of the {@link Class} to load; must not
   * be {@code null}
   *
   * @return a {@link TemplateModel} representing a {@linkplain
   * ClassLoader#loadClass(String) loaded <code>Class</code>}; never
   * {@code null}
   *
   * @exception TemplateModelException if the given class could not be
   * found
   *
   * @see ClassModel
   */
  @Override
  public final TemplateModel get(final String className) throws TemplateModelException {
    TemplateModel returnValue = TemplateModel.NOTHING;
    if (className != null) {
      try {
        returnValue = this.wrap(this.classLoader.loadClass(className));
      } catch (final ClassNotFoundException classNotFoundException) {
        throw new TemplateModelException(classNotFoundException.getMessage(), classNotFoundException);
      }
    }
    return returnValue;
  }
  
}
