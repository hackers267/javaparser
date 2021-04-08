package com.github.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

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

}
