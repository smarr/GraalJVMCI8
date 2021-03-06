/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.internal.jvmci.hotspot.sparc;

import static jdk.internal.jvmci.inittimer.InitTimer.timer;

import java.util.EnumSet;

import jdk.internal.jvmci.code.Architecture;
import jdk.internal.jvmci.code.RegisterConfig;
import jdk.internal.jvmci.code.TargetDescription;
import jdk.internal.jvmci.hotspot.HotSpotCodeCacheProvider;
import jdk.internal.jvmci.hotspot.HotSpotConstantReflectionProvider;
import jdk.internal.jvmci.hotspot.HotSpotJVMCIBackendFactory;
import jdk.internal.jvmci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.internal.jvmci.hotspot.HotSpotMetaAccessProvider;
import jdk.internal.jvmci.hotspot.HotSpotVMConfig;
import jdk.internal.jvmci.inittimer.InitTimer;
import jdk.internal.jvmci.runtime.JVMCIBackend;
import jdk.internal.jvmci.service.ServiceProvider;
import jdk.internal.jvmci.sparc.SPARC;
import jdk.internal.jvmci.sparc.SPARC.CPUFeature;

@ServiceProvider(HotSpotJVMCIBackendFactory.class)
public class SPARCHotSpotJVMCIBackendFactory implements HotSpotJVMCIBackendFactory {

    protected TargetDescription createTarget(HotSpotVMConfig config) {
        final int stackFrameAlignment = 16;
        final int implicitNullCheckLimit = 4096;
        final boolean inlineObjects = false;
        Architecture arch = new SPARC(computeFeatures(config));
        return new TargetDescription(arch, true, stackFrameAlignment, implicitNullCheckLimit, inlineObjects);
    }

    protected HotSpotCodeCacheProvider createCodeCache(HotSpotJVMCIRuntimeProvider runtime, TargetDescription target, RegisterConfig regConfig) {
        return new HotSpotCodeCacheProvider(runtime, runtime.getConfig(), target, regConfig);
    }

    protected EnumSet<CPUFeature> computeFeatures(HotSpotVMConfig config) {
        EnumSet<CPUFeature> features = EnumSet.noneOf(CPUFeature.class);
        if ((config.sparcFeatures & config.vis1Instructions) != 0) {
            features.add(CPUFeature.VIS1);
        }
        if ((config.sparcFeatures & config.vis2Instructions) != 0) {
            features.add(CPUFeature.VIS2);
        }
        if ((config.sparcFeatures & config.vis3Instructions) != 0) {
            features.add(CPUFeature.VIS3);
        }
        if ((config.sparcFeatures & config.cbcondInstructions) != 0) {
            features.add(CPUFeature.CBCOND);
        }
        if (config.useBlockZeroing) {
            features.add(CPUFeature.BLOCK_ZEROING);
        }
        return features;
    }

    @Override
    public String getArchitecture() {
        return "SPARC";
    }

    @Override
    public String toString() {
        return "JVMCIBackend:" + getArchitecture();
    }

    @SuppressWarnings("try")
    public JVMCIBackend createJVMCIBackend(HotSpotJVMCIRuntimeProvider runtime, JVMCIBackend host) {
        assert host == null;
        TargetDescription target = createTarget(runtime.getConfig());

        HotSpotMetaAccessProvider metaAccess = new HotSpotMetaAccessProvider(runtime);
        RegisterConfig regConfig = new SPARCHotSpotRegisterConfig(target, runtime.getConfig());
        HotSpotCodeCacheProvider codeCache = createCodeCache(runtime, target, regConfig);
        HotSpotConstantReflectionProvider constantReflection = new HotSpotConstantReflectionProvider(runtime);
        try (InitTimer rt = timer("instantiate backend")) {
            return createBackend(metaAccess, codeCache, constantReflection);
        }
    }

    protected JVMCIBackend createBackend(HotSpotMetaAccessProvider metaAccess, HotSpotCodeCacheProvider codeCache, HotSpotConstantReflectionProvider constantReflection) {
        return new JVMCIBackend(metaAccess, codeCache, constantReflection);
    }
}
