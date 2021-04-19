package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class AnalyseVo {
    private static final String ROOT_PATH = "/run/media/silence/data/projects/java/trial/mall-persistent/src/main/java/";

    public static List<String> getStringList(String path_string) throws FileNotFoundException {
        File files = new File(path_string);

        CompilationUnit compilationUnit = StaticJavaParser.parse(files);
        List<String> list = new ArrayList<>();
        FieldVisitor fieldVisitor = new FieldVisitor();
        fieldVisitor.visit(compilationUnit, list);
        return list;
    }

    private static String getType(FieldDeclaration c) {
        return c.getVariables()
                .stream()
                .map(VariableDeclarator::getType)
                .map(Type::asString)
                .collect(Collectors.joining());
    }

    private static String getComment(FieldDeclaration c) {
        return c.getAnnotations()
                .stream()
                .filter(x -> Objects.equals(x.getNameAsString(), "ApiModelProperty"))
                .map(x -> {
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
                })
                .collect(Collectors.joining());
    }

    private static String getName(FieldDeclaration c) {
        return c.getVariables()
                .stream()
                .map(x -> x.getName().asString())
                .collect(Collectors.joining());
    }

    private static class FieldVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(FieldDeclaration fd, List<String> arg) {
            super.visit(fd, arg);
            String name = getName(fd);
            String type = getType(fd);
            String comment = getComment(fd);
            String result = name + "|" + type + "|" + comment;
            arg.add(result);
        }
    }
}
