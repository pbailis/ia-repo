package edu.stanford.iattacks;

/**
 * Hello world!
 *
 */

import java.util.*;

import java.util.ArrayList;
import java.util.Map;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;

/**
 * Compute intraprocedural reaching defs of global variables, i.e., the defs are
 * {@link SSAPutInstruction}s on static state.
 *
 * @author manu
 *
 */
public class App {

    private static Iterable<Entrypoint> makePublicEntrypoints(AnalysisScope scope, IClassHierarchy cha, String entryClass) {
        Collection<Entrypoint> result = new ArrayList<Entrypoint>();
        IClass klass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application,
                StringStuff.deployment2CanonicalTypeString(entryClass)));
        for (IMethod m : klass.getDeclaredMethods()) {
            if (m.isPublic()) {
                result.add(new DefaultEntrypoint(m, cha));
            }
        }
        return result;
    }

    /**
     * the exploded control-flow graph on which to compute the analysis
     */
    private final ExplodedControlFlowGraph ecfg;

    private static final boolean VERBOSE = true;
    private static IClassHierarchy cha;

    public App(ExplodedControlFlowGraph ecfg, IClassHierarchy cha) {
        this.ecfg = ecfg;
        this.cha = cha;
    }

    private static AnalysisScope scope;

    public static void main( String[] args ) throws Exception
    {
        scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("target/mahakala-1.0-SNAPSHOT.jar", null);

        cha = ClassHierarchy.make(scope);

        AnalysisCache cache = new AnalysisCache();
        final MethodReference ref = StringStuff.makeMethodReference("edu.stanford.iattacks.Dummy.foo()V");//        MethodReference.findOrCreate(ClassLoaderReference.Application, "Ledu/stanford/iattacks.Dummy", "Dummy.foo",               "()V");
        IMethod method = cha.resolveMethod(ref);

        /*
        // SSacache
        IR ir = cache.getSSACache().findOrCreateIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
        ExplodedControlFlowGraph excfg = ExplodedControlFlowGraph.make(ir);
        */

        AnalysisOptions options = new AnalysisOptions();
        String entryClass = "edu.stanford.iattacks.Dummy";
        String mainClass = null;

        // see also com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
        Iterable<Entrypoint> entrypoints = entryClass != null ? makePublicEntrypoints(scope, cha, entryClass) : Util.makeMainEntrypoints(scope, cha, mainClass);
        options.setEntrypoints(entrypoints);

        options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);
        CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, cache, cha, scope);

        CallGraph cg = builder.makeCallGraph(options, null);

        ICFGSupergraph sg = ICFGSupergraph.make(cg, cache);

        System.out.println("Call sites:");
        System.out.println(sg.getEntriesForProcedure(cg.getNode(method, Everywhere.EVERYWHERE)));

        System.out.println(sg.getIR(cg.getNode(method, Everywhere.EVERYWHERE)));

        IR ir  = sg.getIR(method, Everywhere.EVERYWHERE);
        SSACFG cfg = ir.getControlFlowGraph();
        SSACFG.BasicBlock bb = cfg.getBasicBlock(0);
        SSAInstruction ssai = bb.getAllInstructions().get(0);

        // implement IVisitor pattern, traverse basic blocks until invoke

        for (IBasicBlock ebb : sg.getICFG()) {
            System.out.println(ebb);
        }

        System.out.println( "Hello World!" );
    }
}
