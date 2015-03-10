/**
 * Copyright 2010 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package playn.java;

import java.io.File;
import java.lang.reflect.Method;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

/**
 * Implements the PlayN platform for Java, based on LWJGL. Due to the way LWJGL works, a game must
 * create the platform instance, then perform any of its own initialization that requires access to
 * GL resources, and then call {@link #start} to start the game loop. The {@link #start} call does
 * not return until the game exits.
 */
public class LWJGLPlatform extends JavaPlatform {

  public LWJGLPlatform (Config config) {
    super(config);
  }

  @Override public void setTitle (String title) { Display.setTitle(title); }

  @Override public void start () {
    boolean wasActive = Display.isActive();
    while (!Display.isCloseRequested()) {
      // notify the app if lose or regain focus (treat said as pause/resume)
      boolean newActive = Display.isActive();
      if (wasActive != newActive) {
        lifecycle.emit(wasActive ? Lifecycle.PAUSE : Lifecycle.RESUME);
        wasActive = newActive;
      }
      ((LWJGLGraphics)graphics()).checkScaleFactor();
      // process frame, if we don't need to provide true pausing
      if (newActive || !config.truePause) processFrame();
      Display.update();
      // sleep until it's time for the next frame
      Display.sync(60);
    }

    shutdown();
  }

  @Override protected void preInit () {
    // unpack our native libraries, unless we're running in Java Web Start
    if (!isInJavaWebStart()) {
      SharedLibraryExtractor extractor = new SharedLibraryExtractor();
      File nativesDir = null;
      try {
        nativesDir = extractor.extractLibrary("lwjgl", null).getParentFile();
      } catch (Throwable ex) {
        throw new RuntimeException("Unable to extract LWJGL native libraries.", ex);
      }
      System.setProperty("org.lwjgl.librarypath", nativesDir.getAbsolutePath());
    }
  }

  @Override protected JavaGraphics createGraphics () { return new LWJGLGraphics(this); }
  @Override protected JavaInput createInput () { return new LWJGLInput(this); }

  private boolean isInJavaWebStart () {
    try {
      Method method = Class.forName("javax.jnlp.ServiceManager").
        getDeclaredMethod("lookup", new Class<?>[] { String.class });
      method.invoke(null, "javax.jnlp.PersistenceService");
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }
}
