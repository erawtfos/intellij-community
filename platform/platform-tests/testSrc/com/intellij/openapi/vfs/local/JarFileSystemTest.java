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
package com.intellij.openapi.vfs.local;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.IoTestUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.testFramework.PlatformLangTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;

import static com.intellij.testFramework.PlatformTestUtil.assertPathsEqual;

public class JarFileSystemTest extends PlatformLangTestCase {
  public void testFindFile() throws IOException {
    String rtJarPath = System.getProperty("java.home") + "/lib/rt.jar";

    VirtualFile jarRoot = findByPath(rtJarPath + JarFileSystem.JAR_SEPARATOR);
    assertTrue(jarRoot.isDirectory());

    VirtualFile file2 = findByPath(rtJarPath + JarFileSystem.JAR_SEPARATOR + "java");
    assertTrue(file2.isDirectory());

    VirtualFile file3 = jarRoot.findChild("java");
    assertEquals(file2, file3);

    VirtualFile file4 = findByPath(rtJarPath + JarFileSystem.JAR_SEPARATOR + "java/lang/Object.class");
    assertTrue(!file4.isDirectory());

    byte[] bytes = file4.contentsToByteArray();
    assertNotNull(bytes);
    assertTrue(bytes.length > 10);
  }

  public void testMetaInf() {
    String rtJarPath = System.getProperty("java.home") + "/lib/rt.jar";

    VirtualFile jarRoot = findByPath(rtJarPath + JarFileSystem.JAR_SEPARATOR);
    assertTrue(jarRoot.isDirectory());

    VirtualFile metaInf = jarRoot.findChild("META-INF");
    assertNotNull(metaInf);

    VirtualFile[] children = metaInf.getChildren();
    assertEquals(1, children.length);
    assertEquals("MANIFEST.MF", children[0].getName());
  }

  public void testJarRefresh() throws IOException {
    File jar = IoTestUtil.createTestJar();
    assertTrue(jar.setLastModified(jar.lastModified() - 1000));
    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(jar);
    assertNotNull(vFile);

    final VirtualFile entry = findByPath(jar.getPath() + JarFileSystem.JAR_SEPARATOR + JarFile.MANIFEST_NAME);
    assertContent(entry, "");

    final Ref<Boolean> updated = Ref.create(false);
    ApplicationManager.getApplication().getMessageBus().connect(myTestRootDisposable).subscribe(
      VirtualFileManager.VFS_CHANGES,
      new BulkFileListener.Adapter() {
        @Override
        public void before(@NotNull List<? extends VFileEvent> events) {
          for (VFileEvent event : events) {
            if (event instanceof VFileContentChangeEvent && entry.equals(event.getFile())) {
              updated.set(true);
              break;
            }
          }
        }
      }
    );

    IoTestUtil.writeEntry(jar, JarFile.MANIFEST_NAME, "update");
    vFile.refresh(false, false);

    assertTrue(updated.get());
    assertTrue(entry.isValid());
    assertContent(entry, "update");
  }

  public void testInvalidJar() throws Exception {
    String jarPath = PathManagerEx.getTestDataPath() + "/vfs/maven-toolchain-1.0.jar";
    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(jarPath);
    assertNotNull(vFile);
    VirtualFile manifest = findByPath(jarPath + JarFileSystem.JAR_SEPARATOR + JarFile.MANIFEST_NAME);
    assertNotNull(manifest);
    VirtualFile classFile = findByPath(jarPath + JarFileSystem.JAR_SEPARATOR + "org/apache/maven/toolchain/java/JavaToolChain.class");
    assertNotNull(classFile);
  }

  public void testJarRootForLocalFile() throws Exception {
    String javaHome = System.getProperty("java.home");
    String rtJarPath = javaHome + "/lib/rt.jar";

    VirtualFile rtJarFile = LocalFileSystem.getInstance().findFileByPath(rtJarPath);
    assertNotNull(rtJarFile);
    VirtualFile rtJarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(rtJarFile);
    assertNotNull(rtJarRoot);

    VirtualFile entryFile = findByPath(rtJarPath + JarFileSystem.JAR_SEPARATOR + "java/lang/Object.class");
    VirtualFile entryRoot = JarFileSystem.getInstance().getJarRootForLocalFile(entryFile);
    assertNull(entryRoot);

    VirtualFile nonJarFile = LocalFileSystem.getInstance().findFileByPath(javaHome + "/lib/calendars.properties");
    assertNotNull(nonJarFile);
    VirtualFile nonJarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(nonJarFile);
    assertNull(nonJarRoot);
  }

  private static VirtualFile findByPath(String path) {
    VirtualFile file = JarFileSystem.getInstance().findFileByPath(path);
    assertNotNull(file);
    assertPathsEqual(path, file.getPath());
    return file;
  }

  private static void assertContent(VirtualFile file, String expected) throws IOException {
    String content = new String(file.contentsToByteArray());
    assertEquals(expected, content);
  }
}
