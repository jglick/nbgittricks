/*
 * The MIT License
 *
 * Copyright 2015 CloudBees.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.jglick.gittricks.github_navigator;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class GitHubNavigateActionTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    
    @Test public void normal() throws Exception {
        FileObject r = FileUtil.toFileObject(tmp.getRoot());
        FileObject git = r.createFolder(".git");
        try (OutputStream os = git.createAndOpen("config")) {
            PrintWriter pw = new PrintWriter(os);
            pw.println("[remote \"origin\"]");
            pw.println("	url = git@github.com:owner/repo.git");
            pw.println("	fetch = +refs/heads/*:refs/remotes/origin/*");
            pw.flush();
        }
        try (OutputStream os = git.createAndOpen("HEAD")) {
            PrintWriter pw = new PrintWriter(os);
            pw.println("ref: refs/heads/master");
            pw.flush();
        }
        try (OutputStream os = git.createFolder("refs").createFolder("heads").createAndOpen("master")) {
            PrintWriter pw = new PrintWriter(os);
            pw.println("abc123");
            pw.flush();
        }
        FileObject f = r.createFolder("src").createFolder("stuff").createData("File.txt");
        assertEquals(new URL("https://github.com/owner/repo/blob/abc123/src/stuff/File.txt#L12"), GitHubNavigateAction.urlOf(f, 12, 12));
        assertEquals(new URL("https://github.com/owner/repo/blob/abc123/src/stuff/File.txt#L12-14"), GitHubNavigateAction.urlOf(f, 12, 14));
    }

    @Test public void noGit() throws Exception {
         FileObject r = FileUtil.toFileObject(tmp.getRoot());
         assertNull(GitHubNavigateAction.urlOf(r.createData("f"), 1, 1));
    }

    @Test public void ownerRepo() {
        assertEquals("owner/repo", GitHubNavigateAction.ownerRepo("git@github.com:owner/repo.git"));
    }

}