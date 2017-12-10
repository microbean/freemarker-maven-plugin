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

import java.util.Map;

import freemarker.ext.beans.BeansWrapperConfiguration;

import freemarker.ext.util.ModelFactory;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperConfiguration;
import freemarker.template.Version;

/**
 * A {@link DefaultObjectWrapper} that uses a {@link Map} of {@link
 * ModelFactory} instances indexed by {@link Class} instances to
 * assist in {@linkplain DefaultObjectWrapper#wrap(Object) wrapping
 * <code>Object</code>s}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see #getModelFactory(Class)
 */
public class FlexibleObjectWrapper extends DefaultObjectWrapper {


  /*
   * Instance fields.
   */


  /**
   * A {@link Map} of {@link ModelFactory} instances indexed by the
   * {@link Class} instances for which they are suitable.
   *
   * <p>This field may be {@code null}.</p>
   */
  private final Map<? extends Class<?>, ? extends ModelFactory> modelFactories;


  /*
   * Constructors.
   */


  /**
   * Creates a new {@link FlexibleObjectWrapper}.
   *
   * @param configuration the {@link BeansWrapperConfiguration}
   * describing how this {@link FlexibleObjectWrapper} will be set up;
   * must not be {@code null}
   *
   * @param writeProtected whether the resulting {@link
   * FlexibleObjectWrapper} will be immutable from this point on
   *
   * @param modelFactories a {@link Map} of {@link
   * ModelFactory} instances indexed by {@link Class} instances to
   * assist in {@linkplain DefaultObjectWrapper#wrap(Object) wrapping
   * <code>Object</code>s}; may be {@code null}
   *
   * @see
   * DefaultObjectWrapper#DefaultObjectWrapper(BeansWrapperConfiguration,
   * boolean)
   */
  public FlexibleObjectWrapper(final BeansWrapperConfiguration configuration,
                               final boolean writeProtected,
                               final Map<? extends Class<?>, ? extends ModelFactory> modelFactories) {
    super(configuration, writeProtected);
    this.modelFactories = modelFactories;
  }

  /**
   * Creates a new {@link FlexibleObjectWrapper}.
   *
   * @param configuration the {@link DefaultObjectWrapperConfiguration}
   * describing how this {@link FlexibleObjectWrapper} will be set up;
   * must not be {@code null}
   *
   * @param writeProtected whether the resulting {@link
   * FlexibleObjectWrapper} will be immutable from this point on
   *
   * @param modelFactories a {@link Map} of {@link
   * ModelFactory} instances indexed by {@link Class} instances to
   * assist in {@linkplain DefaultObjectWrapper#wrap(Object) wrapping
   * <code>Object</code>s}; may be {@code null}
   *
   * @see
   * DefaultObjectWrapper#DefaultObjectWrapper(BeansWrapperConfiguration,
   * boolean)
   */
  public FlexibleObjectWrapper(final DefaultObjectWrapperConfiguration configuration,
                               final boolean writeProtected,
                               final Map<? extends Class<?>, ? extends ModelFactory> modelFactories) {
    super(configuration, writeProtected);
    this.modelFactories = modelFactories;
  }
  
  /**
   * Creates a new {@link FlexibleObjectWrapper}.
   *
   * @param version the {@link Version} representing the Freemarker
   * version in use; must not be {@code null}
   *
   * @param modelFactories a {@link Map} of {@link
   * ModelFactory} instances indexed by {@link Class} instances to
   * assist in {@linkplain DefaultObjectWrapper#wrap(Object) wrapping
   * <code>Object</code>s}; may be {@code null}
   *
   * @see
   * DefaultObjectWrapper#DefaultObjectWrapper(BeansWrapperConfiguration,
   * boolean)
   */
  public FlexibleObjectWrapper(final Version version,
                               final Map<? extends Class<?>, ? extends ModelFactory> modelFactories) {
    super(version);
    this.modelFactories = modelFactories;
  }


  /*
   * Instance methods.
   */


  /**
   * Returns a {@link ModelFactory} suitable for the supplied {@link
   * Class}.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * <p>Overrides of this method must never return {@code null}.</p>
   *
   * <p>This implementation tries to return a {@link ModelFactory} as
   * found in the {@linkplain #FlexibleObjectWrapper(Version, Map)
   * <code>Map</code> of such <code>ModelFactory</code> instances
   * provided at construction time}.  If there is no such {@link
   * ModelFactory}, then the return value of invoking the {@link
   * DefaultObjectWrapper#getModelFactory(Class)
   * super.getModelFactory(Class)} is returned instead.</p>
   *
   * @param c the {@link Class} for which a {@link ModelFactory} is
   * desired; must not be {@code null}
   *
   * @return a {@link ModelFactory}; never {@code null}
   *
   * @see DefaultObjectWrapper#getModelFactory(Class)
   */
  @Override
  @SuppressWarnings("rawtypes")
  protected ModelFactory getModelFactory(final Class c) {
    ModelFactory returnValue = null;
    if (c != null && this.modelFactories != null && !this.modelFactories.isEmpty()) {
      returnValue = this.modelFactories.get(c);
    }
    if (returnValue == null) {
      returnValue = super.getModelFactory(c);
    }
    return returnValue;

  }
  
}
