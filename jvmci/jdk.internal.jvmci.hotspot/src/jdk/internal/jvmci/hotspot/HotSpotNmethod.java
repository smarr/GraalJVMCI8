/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jvmci.hotspot;

import static jdk.internal.jvmci.hotspot.CompilerToVM.compilerToVM;
import jdk.internal.jvmci.code.InstalledCode;
import jdk.internal.jvmci.code.InvalidInstalledCodeException;
import jdk.internal.jvmci.meta.JavaKind;
import jdk.internal.jvmci.meta.JavaType;
import jdk.internal.jvmci.meta.ResolvedJavaMethod;

/**
 * Implementation of {@link InstalledCode} for code installed as an nmethod. The nmethod stores a
 * weak reference to an instance of this class. This is necessary to keep the nmethod from being
 * unloaded while the associated {@link HotSpotNmethod} instance is alive.
 * <p>
 * Note that there is no (current) way for the reference from an nmethod to a {@link HotSpotNmethod}
 * instance to be anything but weak. This is due to the fact that HotSpot does not treat nmethods as
 * strong GC roots.
 */
public class HotSpotNmethod extends HotSpotInstalledCode {

    /**
     * This (indirect) Method* reference is safe since class redefinition preserves all methods
     * associated with nmethods in the code cache.
     */
    private final HotSpotResolvedJavaMethod method;

    private final boolean isDefault;
    private final boolean isExternal;

    public HotSpotNmethod(HotSpotResolvedJavaMethod method, String name, boolean isDefault) {
        this(method, name, isDefault, false);
    }

    public HotSpotNmethod(HotSpotResolvedJavaMethod method, String name, boolean isDefault, boolean isExternal) {
        super(name);
        this.method = method;
        this.isDefault = isDefault;
        this.isExternal = isExternal;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public ResolvedJavaMethod getMethod() {
        return method;
    }

    @Override
    public void invalidate() {
        compilerToVM().invalidateInstalledCode(this);
    }

    @Override
    public String toString() {
        return String.format("InstalledNmethod[method=%s, codeBlob=0x%x, isDefault=%b, name=%s]", method, getAddress(), isDefault, name);
    }

    protected boolean checkThreeObjectArgs() {
        assert method.getSignature().getParameterCount(!method.isStatic()) == 3;
        assert method.getSignature().getParameterKind(0) == JavaKind.Object;
        assert method.getSignature().getParameterKind(1) == JavaKind.Object;
        assert !method.isStatic() || method.getSignature().getParameterKind(2) == JavaKind.Object;
        return true;
    }

    private boolean checkArgs(Object... args) {
        JavaType[] sig = method.toParameterTypes();
        assert args.length == sig.length : method.format("%H.%n(%p): expected ") + sig.length + " args, got " + args.length;
        for (int i = 0; i < sig.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                assert sig[i].getJavaKind() == JavaKind.Object : method.format("%H.%n(%p): expected arg ") + i + " to be Object, not " + sig[i];
            } else if (sig[i].getJavaKind() != JavaKind.Object) {
                assert sig[i].getJavaKind().toBoxedJavaClass() == arg.getClass() : method.format("%H.%n(%p): expected arg ") + i + " to be " + sig[i] + ", not " + arg.getClass();
            }
        }
        return true;
    }

    @Override
    public Object executeVarargs(Object... args) throws InvalidInstalledCodeException {
        assert checkArgs(args);
        assert !isExternal();
        return compilerToVM().executeInstalledCode(args, this);
    }

    @Override
    public long getStart() {
        return isValid() ? super.getStart() : 0;
    }
}
