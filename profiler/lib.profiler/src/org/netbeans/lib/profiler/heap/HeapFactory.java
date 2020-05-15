/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.netbeans.lib.profiler.heap;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


/**
 * This is factory class for creating {@link Heap} from the file in Hprof dump format.
 * @author Tomas Hurka
 */
public class HeapFactory {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * this factory method creates lazy {@link Heap} from a memory dump file in Hprof format.
     * <br>
     * Speed: fast (but first method call on the heap object is slow)
     * @param heapDump file which contains memory dump
     * @return implementation of {@link Heap} corresponding to the memory dump
     * passed in heapDump parameter
     * @throws java.io.FileNotFoundException if heapDump file does not exist
     * @throws java.io.IOException if I/O error occurred while accessing heapDump file
     */
    public static Heap createHeap(File heapDump) throws FileNotFoundException, IOException {
        return createHeap(heapDump, 0);
    }

    /**
     * this factory method creates {@link Heap} from a memory dump file in Hprof format.
     * If the memory dump file contains more than one dump, parameter segment is used to
     * select particular dump.
     * <br>
     * Speed: slow
     * @return implementation of {@link Heap} corresponding to the memory dump
     * passed in heapDump parameter
     * @param segment select corresponding dump from multi-dump file
     * @param heapDump file which contains memory dump
     * @throws java.io.FileNotFoundException if heapDump file does not exist
     * @throws java.io.IOException if I/O error occurred while accessing heapDump file
     */
    public static Heap createHeap(File heapDump, int segment)
                           throws FileNotFoundException, IOException {
        return new LazyHeapImpl(heapDump, segment);        
    }

    private static Heap createHeapExplicit(File heapDump, int segment)
                           throws FileNotFoundException, IOException {
        CacheDirectory cacheDir = CacheDirectory.getHeapDumpCacheDirectory(heapDump);
        if (!cacheDir.isTemporary()) {
            File savedDump = cacheDir.getHeapDumpAuxFile();

            if (savedDump.exists() && savedDump.isFile() && savedDump.canRead()) {
                try {
                    return loadHeap(cacheDir);
                } catch (IOException ex) {
                    System.err.println("Loading heap dump "+heapDump+" from cache failed.");
                    ex.printStackTrace(System.err);
                }
            }
        }
        return new HprofHeap(heapDump, segment, cacheDir);

    }
    
    static Heap loadHeap(CacheDirectory cacheDir)
                           throws FileNotFoundException, IOException {
        File savedDump = cacheDir.getHeapDumpAuxFile();
        InputStream is = new BufferedInputStream(new FileInputStream(savedDump), 64*1024);
        DataInputStream dis = new DataInputStream(is);
        Heap heap = new HprofHeap(dis, cacheDir);
        dis.close();
        return heap;
    }

    private static class LazyHeapImpl implements Heap.Lazy {

        private final File heapFile;
        private final int segment;

        private Heap lazy;

        public LazyHeapImpl(File heapFile, int segment) {
            this.heapFile = heapFile;
            this.segment = segment;
        }

        @Override
        public File getHeapFile() {
            return this.heapFile;
        }

        @Override
        public int getHeapSegment() {
            return this.segment;
        }

        private synchronized Heap initLazy() {            
            if (this.lazy == null) {
                try {
                    this.lazy = HeapFactory.createHeapExplicit(heapFile, segment);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.lazy;
        }

        
        @Override
        public List /*<JavaClass>*/ getAllClasses() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getAllClasses();            
        }
    
        @Override
        public List /*<Instance>*/ getBiggestObjectsByRetainedSize(int number) {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getBiggestObjectsByRetainedSize(number);            
        }
    
        @Override
        public GCRoot getGCRoot(Instance instance) {
            Heap heap = (lazy != null) ? lazy : initLazy();
            return heap.getGCRoot(instance);
        }

        @Override
        public Collection /*<GCRoot>*/ getGCRoots() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getGCRoots();
        }

        @Override
        public Instance getInstanceByID(long instanceId) {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getInstanceByID(instanceId);
        }

        @Override
        public JavaClass getJavaClassByID(long javaclassId) {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getJavaClassByID(javaclassId);
        }

        @Override
        public JavaClass getJavaClassByName(String fqn) {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getJavaClassByName(fqn);
        }

        @Override
        public Collection getJavaClassesByRegExp(String regexp) {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getJavaClassesByRegExp(regexp);
        }

        @Override
        public Iterator getAllInstancesIterator() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getAllInstancesIterator();
        }
    
        @Override
        public HeapSummary getSummary() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getSummary();
        }

        @Override
        public Properties getSystemProperties() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.getSystemProperties();
        }

        @Override
        public boolean isRetainedSizeComputed() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.isRetainedSizeComputed();
        }

        @Override
        public boolean isRetainedSizeByClassComputed() {
            Heap instance = (lazy != null) ? lazy : initLazy();
            return instance.isRetainedSizeByClassComputed();
        }

    }
    
}
