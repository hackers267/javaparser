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

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.javaparser.Utils.NamedRequestMapping;
import static com.github.javaparser.Utils.getApiName;

@Slf4j
public class AST {
    public static final String QUERY_PAGE_DATA = "QueryPageData";
    private static final String ROOT_PATH = "/run/media/silence/data/projects/java/zjt-saas/saas-persistent/src/main/java";
    private static final String FILE_PATH = "/run/media/silence/data/projects/java/zjt-saas/saas-admin/src/main/java/com/qhcl/controller/product/ProductInfoController.java";

    public static void main(String[] args) throws FileNotFoundException {
        PropertyConfigurator.configure("log4j.properties");

        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(FILE_PATH));
        String apiPrefix = getApiPrefix(compilationUnit);
        List<String> pageMethods = getPageMethods(compilationUnit);
        HashSet<String> packages = getImport(compilationUnit);
        pageMethods.stream().map(x -> {
            String api = getApi(apiPrefix, x);
            String voPackage = getVoPackage(packages, x);
            String path = getPackageRelativePath(voPackage);
            return api + ":" + path;
        }).forEach(x -> {
            String[] list = x.split(":");
            String api = list[0];
            String path = list[1];
            String abs_path = ROOT_PATH + "/" + path;
            try {
                List<String> filed_list = AnalyseVo.getStringList(abs_path);
                if (filed_list.size() > 0) {
                    String file_name = getPageFileName(api);
                    filed_list.add(0, "#" + api);
                    WriteFile(file_name, filed_list);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    private static String getPageFileName(String api) {
        String file_path = "/run/media/silence/data/projects/webstorm/translate/data";
        return file_path + "/" + String.join("_", api.split("/")) + "_page.txt";
    }

    private static void WriteFile(String file_name, List<String> content) throws IOException {
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

    /**
     * 获取实体包的完整引用
     *
     * @param packages 包的HashSet集合
     * @param x        String
     * @return 包的完整引用
     */
    private static String getVoPackage(HashSet<String> packages, String x) {
        String vo = Arrays.
                stream(x.split(":"))
                .skip(1)
                .collect(Collectors.joining());
        return packages
                .stream()
                .filter(y -> y.endsWith(vo))
                .collect(Collectors.joining());
    }

    /**
     * 获取完整API路径
     *
     * @param apiPrefix String
     * @param x         String
     * @return String 完整API路径
     */
    private static String getApi(String apiPrefix, String x) {
        String key = Arrays
                .stream(x.split(":"))
                .limit(1)
                .collect(Collectors.joining());
        return String.format("%s/%s", apiPrefix, key);
    }

    /**
     * 获取有QueryPageDataVo 的方法对象
     *
     * @param compilationUnit CompilationUnit CompilationUnit对象
     * @return List<String> 有QueryPageDataVo 的方法对象
     */
    private static List<String> getPageMethods(CompilationUnit compilationUnit) {
        StringListMap methods = getMethodParams(compilationUnit);
        return methods.entrySet()
                .stream()
                .filter(x -> {
                    List<String> list = x.getValue();
                    return list.stream().anyMatch(y -> y.contains(QUERY_PAGE_DATA));
                })
                .map(x -> {
                    String key = x.getKey().replaceAll("\"", "");
                    String value = getPageVo(x);
                    return key + ":" + value;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取 Page内的实体对像的String
     *
     * @param x Map.Entry<String,List<String>> Map对象
     * @return String 返回Page内的实体
     */
    private static String getPageVo(Map.Entry<String, List<String>> x) {
        List<String> list = x.getValue();
        return list
                .stream()
                .filter(y -> y.contains(QUERY_PAGE_DATA))
                .map(y -> y.replaceAll(QUERY_PAGE_DATA + "<", ""))
                .map(y -> y.replaceAll(">", ""))
                .collect(Collectors.joining());
    }

    /**
     * 获取java包的相对路径
     *
     * @param x 包名
     * @return 包的相对路径
     */
    private static String getPackageRelativePath(String x) {
        return String.join("/", x.split("\\.")).concat(".java");
    }

    /**
     * 获取Controller的请求根路径
     *
     * @param compilationUnit CompilationUnit 对象
     * @return String Controller的请求根路径
     */
    private static String getApiPrefix(CompilationUnit compilationUnit) {
        VoidVisitor<List<String>> annotationVisitor = new ApiPrefix();
        List<String> apiPrefixes = new ArrayList<>();
        annotationVisitor.visit(compilationUnit, apiPrefixes);
        return apiPrefixes.get(0);
    }

    /**
     * 获取 引入的自定义的实体相对路径 组成的HashSet
     *
     * @param compilationUnit CompilationUnit 对象
     * @return HashSet 引入的自定义的实体相对路径
     */
    private static HashSet<String> getImport(CompilationUnit compilationUnit) {
        HashSet<String> set = new HashSet<>();
        VoidVisitor<HashSet<String>> importVisitor = new ImportVo();
        importVisitor.visit(compilationUnit, set);
        return set;
    }

    /**
     * 获取 方法参数和请求路径对象 组成的Map
     *
     * @param compilationUnit CompilationUnit对象
     * @return StringListMap 方法参数和请求路径对象
     */
    private static StringListMap getMethodParams(CompilationUnit compilationUnit) {
        StringListMap map = new StringListMap();
        VoidVisitor<StringListMap> method = new Method();
        method.visit(compilationUnit, map);
        return map;
    }

    private static class StringListMap extends HashMap<String, List<String>> {
    }

    private static class ApiPrefix extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration ad, List<String> arg) {
            super.visit(ad, arg);
            ad.getAnnotations().forEach(x -> {
                if (x.isSingleMemberAnnotationExpr()) {
                    String api = ((SingleMemberAnnotationExpr) x)
                            .getMemberValue()
                            .toString()
                            .replaceAll("\"", "");
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
            arg.add(id.getNameAsString());
        }
    }
}
