/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.lang.psi.impl;

import com.intellij.openapi.util.Pair;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Max Medvedev on 07/04/14
 */
public class GrMapTypeImpl extends GrMapType {
  private final Map<String, PsiType> myStringEntries;
  private final List<Pair<PsiType, PsiType>> myOtherEntries;

  GrMapTypeImpl(JavaPsiFacade facade,
                GlobalSearchScope scope,
                Map<String, PsiType> stringEntries,
                List<Pair<PsiType, PsiType>> otherEntries,
                LanguageLevel languageLevel) {
    super(facade, scope, languageLevel);
    myStringEntries = stringEntries;
    myOtherEntries = otherEntries;
  }

  @Nullable
  public PsiType getTypeByStringKey(String key) {
    return myStringEntries.get(key);
  }

  @NotNull
  public Set<String> getStringKeys() {
    return myStringEntries.keySet();
  }

  @NotNull
  protected PsiType[] getAllKeyTypes() {
    Set<PsiType> result = new HashSet<PsiType>();
    if (!myStringEntries.isEmpty()) {
      result.add(GroovyPsiManager.getInstance(myFacade.getProject()).createTypeByFQClassName(CommonClassNames.JAVA_LANG_STRING, getResolveScope()));
    }
    for (Pair<PsiType, PsiType> entry : myOtherEntries) {
      result.add(entry.first);
    }
    result.remove(null);
    return result.toArray(createArray(result.size()));
  }

  @NotNull
  protected PsiType[] getAllValueTypes() {
    Set<PsiType> result = new HashSet<PsiType>();
    result.addAll(myStringEntries.values());
    for (Pair<PsiType, PsiType> entry : myOtherEntries) {
      result.add(entry.second);
    }
    result.remove(null);
    return result.toArray(createArray(result.size()));
  }

  @NotNull
  @Override
  protected List<Pair<PsiType, PsiType>> getOtherEntries() {
    return myOtherEntries;
  }

  @NotNull
  @Override
  protected Map<String, PsiType> getStringEntries() {
    return myStringEntries;
  }

  @Override
  public boolean isValid() {
    for (PsiType type : myStringEntries.values()) {
      if (type != null && !type.isValid()) {
        return false;
      }
    }
    for (Pair<PsiType, PsiType> entry : myOtherEntries) {
      if (entry.first != null && !entry.first.isValid()) {
        return false;
      }
      if (entry.second != null && !entry.second.isValid()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isEmpty() {
    return myStringEntries.isEmpty() && myOtherEntries.isEmpty();
  }
}
