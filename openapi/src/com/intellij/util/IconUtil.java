/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.ui.LayeredIcon;
import com.intellij.ide.IconProvider;

import javax.swing.*;


public class IconUtil {
  private static IconProvider[] ourIconProviders = null;

  public static Icon getIcon(VirtualFile file, int flags, Project project) {
    FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);

    Icon providersIcon = getProvidersIcon(file, flags, project);
    Icon icon = providersIcon == null ? file.getIcon() : providersIcon;

    Icon excludedIcon = null;
    Icon lockedIcon = null;
    if (project != null) {
      final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      final boolean isUnderSource = projectFileIndex.isJavaSourceFile(file);
      if (fileType == StdFileTypes.JAVA) {
        if (!isUnderSource) {
          icon = Icons.JAVA_OUTSIDE_SOURCE_ICON;
        }
        else {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
          if (psiFile instanceof PsiJavaFile) {
            PsiClass[] classes = ((PsiJavaFile)psiFile).getClasses();
            if (classes.length != 0) {
              // prefer icon of the class named after file
              final String fileName = file.getNameWithoutExtension();
              for (PsiClass aClass : classes) {
                icon = aClass.getIcon(flags);
                if (Comparing.strEqual(aClass.getName(), fileName)) break;
              }
            }
          }
        }
      }

      if (projectFileIndex.isInSource(file) && CompilerManager.getInstance(project).isExcludedFromCompilation(file)) {
        excludedIcon = Icons.EXCLUDED_FROM_COMPILE_ICON;
      }
    }

    if ((flags & Iconable.ICON_FLAG_READ_STATUS) != 0 && !file.isWritable()) {
      lockedIcon = Icons.LOCKED_ICON;
    }

    if (excludedIcon != null || lockedIcon != null) {
      LayeredIcon layeredIcon = new LayeredIcon(1 + (lockedIcon != null ? 1 : 0) + (excludedIcon != null ? 1 : 0));
      int layer = 0;
      layeredIcon.setIcon(icon, layer++);
      if (lockedIcon != null) {
        layeredIcon.setIcon(lockedIcon, layer++);
      }
      if (excludedIcon != null) {
        layeredIcon.setIcon(excludedIcon, layer);
      }
      icon = layeredIcon;
    }

    return icon;
  }

  public static Icon getProvidersIcon(VirtualFile file, int flags, Project project) {
    if(project == null) return null;

    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

    return psiFile == null ? null : getProvidersIcon(psiFile, flags);
  }

  public static Icon getProvidersIcon(PsiElement element, int flags) {
    for (final IconProvider iconProvider : getIconProviders()) {
      final Icon icon = iconProvider.getIcon(element, flags);
      if (icon != null) return icon;
    }
    return null;
  }

  private static IconProvider[] getIconProviders() {
    if (ourIconProviders == null) {
      ourIconProviders = ApplicationManager.getApplication().getComponents(IconProvider.class);
    }
    return ourIconProviders;
  }
}
