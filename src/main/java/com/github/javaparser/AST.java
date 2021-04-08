package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        map.forEach((x, y) -> {
            System.out.println(x);
            System.out.println(y);
        });
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

        private Function<AnnotationExpr, String> getApiName() {
            return x -> {
                if (x.isSingleMemberAnnotationExpr()) {
                    return ((SingleMemberAnnotationExpr) x).getMemberValue().toString();
                }
                if (x.isNormalAnnotationExpr()) {
                    return ((NormalAnnotationExpr) x)
                            .getPairs()
                            .stream()
                            .filter(y -> Objects.equals(y.getNameAsString(), "value"))
                            .map(MemberValuePair::getValue)
                            .map(Node::toString)
                            .collect(Collectors.joining());
                }
                return "";
            };
        }

        private Predicate<AnnotationExpr> NamedRequestMapping() {
            return x -> Objects.equals(x.getNameAsString(), "RequestMapping");
        }
    }
}
