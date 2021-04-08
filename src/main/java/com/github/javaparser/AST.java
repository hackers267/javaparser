package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.javaparser.Utils.NamedRequestMapping;
import static com.github.javaparser.Utils.getApiName;

@Slf4j
public class AST {
    private static final String FILE_PATH = "/run/media/silence/data/projects/java/trial/mall-admin/src/main/java/com/qhcl/mall/controller/order/OrderManagerController.java";

    public static void main(String[] args) throws FileNotFoundException {
        PropertyConfigurator.configure("log4j.properties");
        AST.class.getResource(".");
        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(FILE_PATH));
        VoidVisitor<List<String>> annotationVisitor = new ApiPrefix();
        List<String> apiPrefixes = new ArrayList<>();
        annotationVisitor.visit(compilationUnit, apiPrefixes);
        String apiPrefix = apiPrefixes.get(0);
        log.info("aprPrefix:{}", apiPrefix);
        StringListMap map = new StringListMap();
        VoidVisitor<StringListMap> method = new Method();
        method.visit(compilationUnit, map);

        HashSet<String> set = new HashSet<>();
        VoidVisitor<HashSet<String>> importVisitor = new ImportVo();
        importVisitor.visit(compilationUnit, set);
    }

    private static class StringListMap extends HashMap<String, List<String>> {
    }

    private static class ApiPrefix extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration ad, List<String> arg) {
            super.visit(ad, arg);
            ad.getAnnotations().forEach(x -> {
                if (x.isSingleMemberAnnotationExpr()) {
                    String api = ((SingleMemberAnnotationExpr) x).getMemberValue().toString();
                    arg.add(api);
                }
            });
        }
    }

    private static class Method extends VoidVisitorAdapter<StringListMap> {
        @Override
        public void visit(MethodDeclaration md, StringListMap arg) {
            super.visit(md, arg);
            String api = md.getAnnotations()
                    .stream()
                    .filter(NamedRequestMapping())
                    .map(getApiName())
                    .collect(Collectors.joining());
            List<String> parameters = md.getParameters()
                    .stream()
                    .map(Parameter::getType)
                    .map(Node::toString)
                    .collect(Collectors.toList());
            arg.put(api, parameters);
        }
    }

    private static class ImportVo extends VoidVisitorAdapter<HashSet<String>> {
        @Override
        public void visit(ImportDeclaration id, HashSet<String> arg) {
            super.visit(id, arg);
            if (id.getNameAsString().contains("vo")) {
                arg.add(id.getNameAsString());
            }
        }
    }
}
