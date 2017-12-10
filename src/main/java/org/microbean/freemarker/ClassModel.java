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

import java.lang.annotation.Annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.Objects;

import freemarker.ext.beans.ArrayModel;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel; // for javadoc only
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * A {@link BeanModel} that permits additional operations from within
 * a Freemarker template on {@link Class} objects.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see BeanModel
 */
public class ClassModel extends BeanModel implements TemplateScalarModel {


  /*
   * Instance fields.
   */


  /**
   * The {@link Class} to model.
   *
   * <p>This field is never {@code null}.</p>
   */
  private final Class<?> c;


  /*
   * Constructors.
   */

  
  /**
   * Creates a new {@link ClassModel}.
   *
   * @param c the {@link Class} to model; must not be {@code null}
   *
   * @param beansWrapper a {@link BeansWrapper} that will be used by
   * the {@linkplain BeanModel superclass}; see its documentation for
   * details; must not be {@code null}
   *
   * @see BeanModel#BeanModel(Object, BeansWrapper)
   *
   * @exception NullPointerException if {@code c} or {@code
   * beansWrapper} is {@code null}
   */
  public ClassModel(final Class<?> c, final BeansWrapper beansWrapper) {
    super(c, beansWrapper);
    Objects.requireNonNull(c);
    this.c = c;
  }


  /*
   * Instance methods.
   */
  

  /**
   * Returns {@code false} when invoked because a {@link Class} as
   * modeled here is never "empty" of its attributes.
   *
   * @return {@code false} when invoked
   */
  @Override
  public final boolean isEmpty() {
    return false;
  }

  /**
   * Returns various {@link TemplateModel} implementations depending
   * on the {@code key} passed.
   *
   * @param key the name of some conceptual attribute of a {@link
   * Class} that might be useful in a Freemarker template, such as
   * {@code annotations}, {@code declaredFields}, {@code
   * declaredMethods}, {@code fields} and {@code methods}; if not
   * directly handled by this class, it may be {@linkplain
   * BeanModel#get(String) handled by the superclass}; must not be
   * {@code null}
   *
   * @return a {@link TemplateModel} in accordance with the general
   * contract of {@link TemplateHashModel#get(String)}
   *
   * @exception NullPointerException if {@code key} is {@code null}
   *
   * @exception TemplateModelException if any other error occurs
   *
   * @see BeanModel#get(String)
   *
   * @see TemplateHashModel#get(String)
   */
  @Override
  public final TemplateModel get(final String key) throws TemplateModelException {
    TemplateModel returnValue = null;
    if ("annotations".equals(key)) {
      final Annotation[] annotations = this.c.getAnnotations();
      assert annotations != null;
      final SimpleHash annotationMap = new SimpleHash(this.wrapper);
      for (final Annotation annotation : annotations) {
        if (annotation != null) {
          final Class<?> annotationType = annotation.annotationType();
          annotationMap.put(annotationType.getName(), annotation);
        }
      }
      returnValue = annotationMap;
    } else if ("declaredFields".equals(key)) {
      final Field[] fields = this.c.getDeclaredFields();
      assert fields != null;
      final SimpleHash fieldMap = new SimpleHash(this.wrapper);
      for (final Field field : fields) {
        if (field != null) {
          fieldMap.put(field.getName(), field);
        }
      }
      returnValue = fieldMap;
    } else if ("declaredMethods".equals(key)) {
      final Method[] methods = this.c.getDeclaredMethods();
      assert methods != null;
      final SimpleHash methodMap = new SimpleHash(this.wrapper);
      for (final Method method : methods) {
        if (method != null) {
          // XXX TODO FIXME: you can have overloaded methods of course; name is not sufficiently unique
          methodMap.put(method.getName(), method);
        }
      }
      returnValue = methodMap;
    } else if ("fields".equals(key)) {
      final Field[] fields = this.c.getFields();
      assert fields != null;
      final SimpleHash fieldMap = new SimpleHash(this.wrapper);
      for (final Field field : fields) {
        if (field != null) {
          fieldMap.put(field.getName(), field);
        }
      }
      returnValue = fieldMap;
    } else if ("methods".equals(key)) {
      final Method[] methods = this.c.getMethods();
      assert methods != null;
      final SimpleHash methodMap = new SimpleHash(this.wrapper);
      for (final Method method : methods) {
        if (method != null) {
          // XXX TODO FIXME: you can have overloaded methods of course; name is not sufficiently unique
          methodMap.put(method.getName(), method);
        }
      }
      returnValue = methodMap;
    } else {
      returnValue = super.get(key);
    }
    return returnValue;
  }

  /**
   * Returns the {@linkplain Class#getName() name} of the {@link
   * Class} supplied {@linkplain #ClassModel(Class, BeansWrapper) at
   * construction time}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @return the name of the {@link Class} being modeled; never {@code
   * null}
   */
  @Override
  public final String getAsString() {
    return this.c.getName();
  }

}
