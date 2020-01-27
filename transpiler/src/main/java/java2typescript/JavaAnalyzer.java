/**
 * Copyright 2017 The Java2TypeScript Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java2typescript;

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.JavaContainerProvider;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.*;
import com.intellij.lang.MetaLanguage;
import com.intellij.lang.jvm.facade.JvmElementProvider;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.augment.TypeAnnotationModifier;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.compiled.ClsStubBuilder;
import com.intellij.psi.impl.JavaClassSupersImpl;
import com.intellij.psi.impl.PsiElementFinderImpl;
import com.intellij.psi.impl.PsiNameHelperImpl;
import com.intellij.psi.impl.PsiTreeChangePreprocessor;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.impl.java.stubs.ClsStubPsiFactory;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.meta.MetaDataContributor;
import com.intellij.psi.stubs.BinaryFileStubBuilders;
import com.intellij.psi.util.JavaClassSupers;

import java.io.File;

public class JavaAnalyzer {

    private JavaCoreApplicationEnvironment appEnvironment;
    private JavaCoreProjectEnvironment javaEnvironment;

    public JavaAnalyzer() {
        Disposable d = () -> {
        };

        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy.class);



        ExtensionsArea area = Extensions.getRootArea();
        CoreApplicationEnvironment.registerExtensionPoint(area, BinaryFileStubBuilders.EP_NAME, FileTypeExtensionPoint.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, FileContextProvider.EP_NAME, FileContextProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, MetaDataContributor.EP_NAME, MetaDataContributor.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, ContainerProvider.EP_NAME, ContainerProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, TypeAnnotationModifier.EP_NAME, TypeAnnotationModifier.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, MetaLanguage.EP_NAME, MetaLanguage.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, JavaModuleSystem.EP_NAME, JavaModuleSystem.class);

        CoreApplicationEnvironment.registerApplicationExtensionPoint(ContainerProvider.EP_NAME, JavaContainerProvider.class);

        appEnvironment = new JavaCoreApplicationEnvironment(d);
        appEnvironment.registerApplicationService(JavaClassSupers.class, new JavaClassSupersImpl());

        javaEnvironment = new JavaCoreProjectEnvironment(d, appEnvironment) {
            @Override
            protected void preregisterServices() {
                ExtensionsArea a = Extensions.getArea(myProject);
                CoreApplicationEnvironment.registerExtensionPoint(a, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor.class);
                CoreApplicationEnvironment.registerExtensionPoint(a, PsiElementFinder.EP_NAME, PsiElementFinder.class);
                CoreApplicationEnvironment.registerExtensionPoint(a, JvmElementProvider.EP_NAME, JvmElementProvider.class);
            }
            @Override
            protected void registerJavaPsiFacade() {
                JavaFileManager javaFileManager = getProject().getComponent(JavaFileManager.class);
                CoreJavaFileManager coreJavaFileManager = (CoreJavaFileManager) javaFileManager;
                ServiceManager.getService(getProject(), CoreJavaFileManager.class);
                getProject().registerService(CoreJavaFileManager.class, coreJavaFileManager);
                getProject().registerService(PsiNameHelper.class, PsiNameHelperImpl.getInstance());
                PsiElementFinder finder = new PsiElementFinderImpl(getProject(), coreJavaFileManager);
                ExtensionsArea area = Extensions.getArea(getProject());
                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(finder);
                super.registerJavaPsiFacade();
            }
        };
        System.setProperty("idea.use.native.fs.for.win", "false");
    }

    public void addClasspath(String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            if (f.isDirectory()) {
                javaEnvironment.addSourcesToClasspath(javaEnvironment.getEnvironment().getLocalFileSystem().findFileByIoFile(f));
            } else {
                javaEnvironment.addJarToClassPath(f);
            }
        }
    }

    public PsiDirectory analyze(File srcDir) {
        VirtualFile vf = javaEnvironment.getEnvironment().getLocalFileSystem().findFileByIoFile(srcDir);
        if (vf != null) {
            javaEnvironment.addSourcesToClasspath(vf);
            return PsiManager.getInstance(javaEnvironment.getProject()).findDirectory(vf);
        }
        return null;
    }

}
