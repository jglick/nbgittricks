/*
 * The MIT License
 *
 * Copyright 2014 CloudBees.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.text.StyledDocument;
import org.netbeans.api.annotations.common.CheckForNull;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(category="Git", id="com.github.jglick.gittricks.github_navigator.GitHubNavigateAction")
@ActionRegistration(displayName="#CTL_GitHubNavigateAction", lazy=true)
@ActionReference(path="Menu/GoTo", position=2900)
@Messages("CTL_GitHubNavigateAction=Open on GitHub")
public final class GitHubNavigateAction implements ActionListener {
    
    private final EditorCookie context;

    public GitHubNavigateAction(EditorCookie context) {
        this.context = context;
    }

    @Override public void actionPerformed(ActionEvent ev) {
        if (context == null) {
            // TODO allow context to be a DataObject instead
            StatusDisplayer.getDefault().setStatusText("Open file in editor to use.");
            return;
        }
        JEditorPane pane = context.getOpenedPanes()[0];
        int selStart = pane.getSelectionStart();
        int selEnd = pane.getSelectionEnd();
        StyledDocument doc = context.getDocument();
        int startLine = NbDocument.findLineNumber(doc, Math.min(selStart, selEnd)) + 1;
        int endLine = Math.max(startLine, NbDocument.findLineNumber(doc, Math.max(selStart, selEnd)) + (NbDocument.findLineColumn(doc, Math.max(selStart, selEnd)) == 0 ? 0 : 1));
        FileObject f = ((DataObject) doc.getProperty(StyledDocument.StreamDescriptionProperty)).getPrimaryFile(); // TODO is there no helper method for this?
        URL u;
        try {
            u = urlOf(f, startLine, endLine);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return;
        }
        if (u == null) {
            StatusDisplayer.getDefault().setStatusText("Cannot find " + FileUtil.getFileDisplayName(f) + " on GitHub.");
            return;
        }
        HtmlBrowser.URLDisplayer.getDefault().showURLExternal(u);
    }

    static @CheckForNull URL urlOf(FileObject f, int startLine, int endLine) throws IOException {
        FileObject d = f.getParent();
        return urlOf(d, f.getNameExt(), startLine, endLine);
    }
    private static URL urlOf(FileObject d, String path, int startLine, int endLine) throws IOException {
        FileObject git = d.getFileObject(".git");
        if (git == null) {
            FileObject d2 = d.getParent();
            if (d2 == null) {
                return null;
            } else {
                return urlOf(d2, d.getNameExt() + "/" + path, startLine, endLine);
            }
        }
        FileObject config = git.getFileObject("config");
        if (config == null) {
            return null;
        }
        String ownerRepo = null;
        for (String l : config.asLines()) {
            Matcher m = GIT_CONFIG_URL.matcher(l);
            if (m.matches()) {
                ownerRepo = ownerRepo(m.group(1));
                break;
            }
        }
        if (ownerRepo == null) {
            return null;
        }
        FileObject head = git.getFileObject("HEAD");
        if (head == null) {
            return null;
        }
        String ref = null;
        for (String l : head.asLines()) {
            Matcher m = REF.matcher(l);
            if (m.matches()) {
                ref = m.group(1);
                break;
            }
        }
        if (ref == null) {
            return null;
        }
        FileObject refF = git.getFileObject(ref);
        if (refF == null) {
            return null;
        }
        String commit = refF.asText().trim();
        return new URL("https://github.com/" + ownerRepo + "/blob/" + commit + "/" + path + "#L" + (startLine == endLine ? startLine : startLine + "-L" + endLine));
    }

    static @CheckForNull String ownerRepo(String url) {
        for (Pattern p : GITHUB_URLS) {
            Matcher m = p.matcher(url);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    private static final Pattern GIT_CONFIG_URL = Pattern.compile("\\s*url\\s*=\\s*(.+)\\s*");
    private static final Pattern[] GITHUB_URLS = {
        Pattern.compile("(?:git@github[.]com:|ssh://git@github[.]com/)([^/]+/[^/]+?)([.]git)?"),
        Pattern.compile("https://github.com/([^/]+/[^/]+?)([.]git|/|)"),
        Pattern.compile("git://github.com/([^/]+/[^/]+?)([.]git|/|)"),
    };
    private static final Pattern REF = Pattern.compile("ref: (.+)");

}
