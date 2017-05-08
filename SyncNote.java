package com.DCMMC.util;

import java.io.*;
import java.nio.file.Files;

/**
 * @author DCMMC
 * @since 1.7
 * class comment : A simple CLI tool to sync Note to Markdown file from Source Code's comments
 * Created by DCMMC on 2017/5/8.
 */
public class SyncNote {
    private static File markdownFile = null;
    /**
     *
     * @param args arg[0]: sourceCodeFile(with suffix .c, .cpp or .java)
     *             arg[1]: markdownNoteFile(with suffix .md)
     *             e.g. SyncNote source.java note.md
     *             Or type SyncNote -h/--help to get help info.
     */
    public static void main(String[] args) {
        /**
         * For DEBUG...
         */
        //String[] args = {"SyncNote.java", "markdown.md"};
        ///测试Note 1

        //一些命令行参数检测
        try {
            if(args.length == 1) {
                if(args[0].startsWith("-h") || args[0].equals("--help")) {
                    System.out.println("Usage: SyncNote sourceCodeFile.[c|cpp|java] markdownNoteFile.md");
                    System.exit(1);
                } else {
                    System.out.println("Invalid arguments. Press SyncNote -h or sync --help to get help info.");
                    System.exit(1);
                }
            } else if(args.length == 2 && validArgs(args[0], args[1]))
                        if(syncNoteToMarkdown(args[0], "#### 链接")) {
                            System.out.println("Success sync Note from Source "+args[0]+" to Markdown File "
                            + args[1]);
                        } else {
                            System.out.println("Failure sync Note To Markdown File.");
                            System.exit(1);
                        }
                    else {
                        System.out.println("Invalid arguments. Press SyncNote -h or sync --help to get help info.");
                        System.exit(1);
                    }
        } catch (FileNotFoundException fe) {
            System.out.println("File Not Found :"+fe+" !");
            throw new RuntimeException("FileNotFound");
        } catch (Exception e) {
            System.out.println("Oops... There is a problem when trying to sync Note. "+ e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private static boolean validArgs(String source, String markdown) throws Exception {
        File sourceFile = new File(source);
        markdownFile = new File(markdown);

        if(!source.matches("\\w+\\.(c|cpp|java)$") || !markdown.matches("\\w+\\.md$"))
            return false;

        if(!sourceFile.exists()) {
            //For DEBUG...
            //System.out.println(sourceFile.getAbsolutePath());
            //原来这里如果读取的是相对路径的话, 是基于E:\\DCMMC\\Java\\Java SE8的
            return false;
        } else if(!sourceFile.canRead()) {
            throw new Exception("Cannot read File "+source);
        }

        if(!markdownFile.exists()) {
            System.out.println("The file "+markdown+" do not exists. "
                    +"Try to create a new markdown file named "+markdown+"...");
            if (!markdownFile.createNewFile())
                throw new Exception("Cannot create file "+markdown);
        } else if(!markdownFile.canRead() || (!markdownFile.canWrite() || !markdownFile.setWritable(true))) {
            throw new Exception("Permission denial when trying to read or write file "+markdown);
        }

        return true;
    }

    /**
     *
     * @param source
     *        SourceCodeFile with suffix .md 不验证文件是否存在
     * @param regex
     *        将Source中的Note插入在指定行的前面, 支持正则表达式
     * @return
     *        写入Markdown文件成功就返回true
     * @throws Exception
     *         各种异常
     */
    private static boolean syncNoteToMarkdown(String source, String regex) throws Exception {
        //读取Source 文件
        BufferedReader sourceIn = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
        //临时文件
        File tempOutFile = File.createTempFile("tmpMarkdown", ".tmp");
        //从Markdown文件中输入
        FileInputStream fis = new FileInputStream(markdownFile);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
        //输出到tmp文件
        FileOutputStream fos = new FileOutputStream(tempOutFile);
        PrintWriter out = new PrintWriter(fos);//一行一行的输入

        //从第一行开始
        int lineNum = 1;
        //只需要在第一处匹配的行之前插入Note
        boolean successFindFirst = false;
        //每一行的数据
        String thisLine;
        while((thisLine = in.readLine()) != null) {
            //按照regex的内容找到那一行
            if(thisLine.matches(regex) && !successFindFirst) {
                successFindFirst = true;
                //从Source中读取///开头的注释中的内容
                String sourceLine;
                while((sourceLine = sourceIn.readLine()) != null) {
                    //不能包含我这个代码里面的/ / / 还有我和风格的文件终止符号///:~
                    if (sourceLine.contains("///") && !sourceLine.contains("\"///\"") && !sourceLine.contains("///:~")) {
                        //sourceLine.indexOf返回的是匹配倒是subString的第一个字符的offset
                        out.println(sourceLine.substring(sourceLine.indexOf("///") + 3));
                        lineNum++;
                    }
                }
            }
            //还没找到那一行就把这一行保存在temp文件中
            out.println(thisLine);
            lineNum++;
        }
        //如果整个Markdown文件都没有匹配到#### 链接的话, 就在文件最后面插入Note
        if(!successFindFirst) {
            //从Source中读取///开头的注释中的内容
            String sourceLine;
            while((sourceLine = sourceIn.readLine()) != null) {
                //不能包含我这个代码里面的/ / /
                if (sourceLine.contains("///") && !sourceLine.contains("\"///\"")) {
                    //sourceLine.indexOf返回的是匹配倒是subString的第一个字符的offset
                    out.println(sourceLine.substring(sourceLine.indexOf("///") + 3));
                    lineNum++;
                }
                successFindFirst = true;
            }
        }

        //安全删除原始的markdown文件
        File markdownBak = new File(markdownFile.getCanonicalPath()+".bak");
        //清除旧的备份
        if(markdownBak.exists()) {
            if(markdownBak.setWritable(true) && markdownBak.delete()) {
                System.out.println("Find old "+markdownFile.getName()+" backup file, delete old bak successful");
            } else {
                System.out.println("Find old "+markdownFile.getName()+" backup file, but delete old bak fail.");
                throw new IOException("Delete old backup file failed.");
            }
        }
        //备份
        Files.copy(markdownFile.toPath(), markdownBak.toPath());
        //一定要记得关闭Markdown文件再删除
        in.close();
        //各种关闭
        //TODO: 改成try-resource子句
        out.close();
        sourceIn.close();
        fis.close();
        fos.close();
        //删除原来的Markdown文件
        if(markdownFile.setWritable(true) && markdownFile.delete()) {
            System.out.println("Delete file "+markdownFile+" successful.");
        } else {
            throw new IOException("Delete old markdown file "+markdownFile.getName()+" failed.");
        }
        //把临时文件改名为原文件名 然而这个tmp文件在C盘... 这个方法不行啊.
        //if(!tempOutFile.renameTo(markdownFile)) {
        //    throw new IOException("Failed to rename tempMarkdown File to "+markdownFile.getName());
        //}

        //直接把临时文件复制到当前目录来
        Files.copy(tempOutFile.toPath(), markdownFile.toPath());

        System.out.println("The total lines in "+markdownFile.getName()+" is "+lineNum);

        return successFindFirst;
    }
}///:~
