/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.calc;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;

@NodeInfo(shortName = "-")
public class IntegerSubNode extends IntegerArithmeticNode implements Canonicalizable {

    public IntegerSubNode(Kind kind, ValueNode x, ValueNode y) {
        super(kind, x, y);
    }

    @Override
    public boolean inferStamp() {
        return updateStamp(StampTool.sub(x().stamp(), y().stamp()));
    }

    @Override
    public Constant evalConst(Constant... inputs) {
        assert inputs.length == 2;
        return Constant.forIntegerKind(kind(), inputs[0].asLong() - inputs[1].asLong(), null);
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (x() == y()) {
            return ConstantNode.forIntegerKind(kind(), 0, graph());
        }
        if (x() instanceof IntegerAddNode) {
            IntegerAddNode x = (IntegerAddNode) x();
            if (x.y() == y()) {
                // (a + b) - b
                return x.x();
            }
            if (x.x() == y()) {
                // (a + b) - a
                return x.y();
            }
        } else if (x() instanceof IntegerSubNode) {
            IntegerSubNode x = (IntegerSubNode) x();
            if (x.x() == y()) {
                // (a - b) - a
                return graph().unique(new NegateNode(x.y()));
            }
        }
        if (y() instanceof IntegerAddNode) {
            IntegerAddNode y = (IntegerAddNode) y();
            if (y.x() == x()) {
                // a - (a + b)
                return graph().unique(new NegateNode(y.y()));
            }
            if (y.y() == x()) {
                // b - (a + b)
                return graph().unique(new NegateNode(y.x()));
            }
        } else if (y() instanceof IntegerSubNode) {
            IntegerSubNode y = (IntegerSubNode) y();
            if (y.x() == x()) {
                // a - (a - b)
                return y.y();
            }
        }
        if (x().isConstant() && y().isConstant()) {
            return ConstantNode.forPrimitive(evalConst(x().asConstant(), y().asConstant()), graph());
        } else if (y().isConstant()) {
            long c = y().asConstant().asLong();
            if (c == 0) {
                return x();
            }
            BinaryNode reassociated = BinaryNode.reassociate(this, ValueNode.isConstantPredicate());
            if (reassociated != this) {
                return reassociated;
            }
            if (c < 0) {
                if (kind() == Kind.Int) {
                    return IntegerArithmeticNode.add(x(), ConstantNode.forInt((int) -c, graph()));
                } else {
                    assert kind() == Kind.Long;
                    return IntegerArithmeticNode.add(x(), ConstantNode.forLong(-c, graph()));
                }
            }
        } else if (x().isConstant()) {
            long c = x().asConstant().asLong();
            if (c == 0) {
                return graph().unique(new NegateNode(y()));
            }
            return BinaryNode.reassociate(this, ValueNode.isConstantPredicate());
        }
        if (y() instanceof NegateNode) {
            return IntegerArithmeticNode.add(x(), ((NegateNode) y()).x());
        }
        return this;
    }

    @Override
    public void generate(ArithmeticLIRGenerator gen) {
        gen.setResult(this, gen.emitSub(gen.operand(x()), gen.operand(y())));
    }
}
