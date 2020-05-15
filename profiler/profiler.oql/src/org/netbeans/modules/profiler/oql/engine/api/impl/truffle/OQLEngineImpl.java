package org.netbeans.modules.profiler.oql.engine.api.impl.truffle;

import com.oracle.truffle.heap.HeapLanguage;
import com.oracle.truffle.heap.HeapUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

public class OQLEngineImpl {

    private final Context ctx;

    public OQLEngineImpl(File heapFile) throws IOException {
        this.ctx = Context.newBuilder().allowAllAccess(true).allowPolyglotAccess(PolyglotAccess.ALL).build();
        ctx.initialize("heap");
        ctx.initialize("js");

        ctx.getBindings("heap").getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("js");
        ctx.getBindings("heap").getMember(HeapLanguage.Globals.BIND_GLOBAL_SYMBOLS).execute(ctx.getBindings("js"));

        Source heapSrc = Source.newBuilder("heap", HeapUtils.bytesOf(heapFile), heapFile.getName())
                .uri(heapFile.toURI())
                .mimeType("application/x-netbeans-profiler-hprof").build();
        Value heap = this.ctx.eval(heapSrc);
        ctx.getBindings("js").putMember("heap", heap);
    }

    public static boolean isOQLSupported() {
        return true;    // OQL should be always available, although it might be slow in interpreted mode
    }

    public void executeJsQuery(String javascript) throws IOException {
        Source querySrc = Source.newBuilder("js", javascript, "fn.js").build();
        ctx.eval(querySrc);
    }

    public void executeQuery(String query, OQLEngine.ObjectVisitor visitor) throws OQLException {
        try {
            OQLQuery parsed = parseQuery(query);
            if (parsed == null) {
                executeJsQuery(query);
                return;
            }
            visitor = (visitor == null) ? OQLEngine.ObjectVisitor.DEFAULT : visitor;
            ctx.getBindings("js").putMember("visitor", visitor);
            //System.out.println("Query:"+parsed.buildJS());
            Source querySrc = Source.newBuilder("js", parsed.buildJS(), "fn.js").build();
            ctx.eval(querySrc);
        } catch (IOException e) {
            throw new OQLException("Cannot execute JavaScript query.", e);
        }
    }

    public void cancelQuery() {
        // TODO: not implemented...
    }

    public Object unwrapJavaObject(Object obj) {
        return obj; // TODO: find where used...
    }

    public Object unwrapJavaObject(Object obj, boolean tryAssociative) {
        return obj; // TODO
    }

    public boolean isCancelled() {
        return false;
    }

    public OQLQuery parseQuery(String query) throws OQLException {
        StringTokenizer st = new StringTokenizer(query);
        if (st.hasMoreTokens()) {
            String first = st.nextToken();
            if (!first.equals("select")) { // NOI18N
                // Query does not start with 'select' keyword.
                // Just treat it as plain JavaScript and eval it.
                return null;
            }
        } else {
            throw new OQLException("TODO");
        }

        StringBuilder selectExpr = new StringBuilder(); // NOI18N
        boolean seenFrom = false;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("from")) { // NOI18N
                seenFrom = true;
                break;
            }
            selectExpr.append(" ").append(tok); // NOI18N
        }

        if (selectExpr.length() == 0) { // NOI18N
            throw new OQLException("TODO");
        }

        String className = null;
        boolean isInstanceOf = false;
        StringBuilder whereExpr = null;
        String identifier = null;

        if (seenFrom) {
            if (st.hasMoreTokens()) {
                String tmp = st.nextToken();
                if (tmp.equals("instanceof")) { // NOI18N
                    isInstanceOf = true;
                    if (!st.hasMoreTokens()) {
                        throw new OQLException("TODO");
                    }
                    className = st.nextToken();
                } else {
                    className = tmp;
                }
            } else {
                throw new OQLException("TODO");
            }

            if (st.hasMoreTokens()) {
                identifier = st.nextToken();
                if (identifier.equals("where")) { // NOI18N
                    throw new OQLException("TODO");
                }
                if (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    if (!tmp.equals("where")) { // NOI18N
                        throw new OQLException("TODO");
                    }

                    whereExpr = new StringBuilder();  // NOI18N
                    while (st.hasMoreTokens()) {
                        whereExpr.append(" ").append(st.nextToken()); // NOI18N
                    }
                    if (whereExpr.length() == 0) { // NOI18N
                        throw new OQLException("TODO");
                    }
                }
            } else {
                throw new OQLException("TODO");
            }
        }
        return new OQLQuery(selectExpr.toString(), isInstanceOf, className, identifier, whereExpr != null ? whereExpr.toString() : null);
    }

}

