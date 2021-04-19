package com.github.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utils {
    public static Function<AnnotationExpr, String> getApiName() {
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

    public static Predicate<AnnotationExpr> NamedRequestMapping() {
        return x -> Objects.equals(x.getNameAsString(), "RequestMapping");
    }

    static void WriteFile(String file_name, List<String> content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file_name));
        content.forEach(x -> {
            try {
                writer.write(x + System.getProperty("line.separator"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();

    }
}
