/*
 * Copyright 2002-2007 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ide.eclipse.beans.ui.editor.contentassist.requestor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.Signature;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.springframework.ide.eclipse.beans.core.BeansCorePlugin;
import org.springframework.ide.eclipse.beans.core.model.IBean;
import org.springframework.ide.eclipse.beans.ui.editor.contentassist.BeansJavaCompletionProposal;
import org.springframework.ide.eclipse.beans.ui.editor.outline.DelegatingLabelProvider;
import org.springframework.ide.eclipse.beans.ui.editor.util.BeansEditorUtils;
import org.springframework.ide.eclipse.beans.ui.model.BeansModelImages;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@SuppressWarnings("restriction")
public class BeanReferenceSearchRequestor {

    public static final int TYPE_MATCHING_RELEVANCE = 20;

    public static final int RELEVANCE = 10;

    protected Set<String> beans;

    protected ContentAssistRequest request;

    protected List<String> requiredTypes = null;

    public BeanReferenceSearchRequestor(ContentAssistRequest request) {
        this(request, new ArrayList<String>());
    }

    public BeanReferenceSearchRequestor(ContentAssistRequest request, List<String> requiredTypes) {
        this.request = request;
        this.beans = new HashSet<String>();
        this.requiredTypes = requiredTypes;
    }

    public void acceptSearchMatch(IBean bean, IFile file, String prefix) {
        if (bean.getElementName() != null
                && bean.getElementName().toLowerCase().startsWith(prefix.toLowerCase())) {
            String beanName = bean.getElementName();
            String replaceText = beanName;
            String fileName = bean.getElementResource().getProjectRelativePath().toString();
            String key = beanName + fileName;
            if (!beans.contains(key)) {
                StringBuffer buf = new StringBuffer();
                buf.append(beanName);
                if (bean.getClassName() != null) {
                    String className = bean.getClassName();
                    buf.append(" [");
                    buf.append(Signature.getSimpleName(className));
                    buf.append("]");
                }
                if (bean.getParentName() != null) {
                    buf.append(" <");
                    buf.append(bean.getParentName());
                    buf.append(">");
                }
                buf.append(" - ");
                buf.append(fileName);
                String displayText = buf.toString();

                Image image = BeansModelImages.getImage(bean, BeansCorePlugin
						.getModel().getConfig(file));
                BeansJavaCompletionProposal proposal = null;
                if (this.requiredTypes.contains(bean.getClassName())) {
                    proposal = new BeansJavaCompletionProposal(replaceText, request
                            .getReplacementBeginPosition(), request.getReplacementLength(),
                            replaceText.length(), image, displayText, null, BeansEditorUtils
                                    .createAdditionalProposalInfo(bean),
                            BeanReferenceSearchRequestor.TYPE_MATCHING_RELEVANCE);
                }
                else {
                    proposal = new BeansJavaCompletionProposal(replaceText, request
                            .getReplacementBeginPosition(), request.getReplacementLength(),
                            replaceText.length(), image, displayText, null, BeansEditorUtils
                                    .createAdditionalProposalInfo(bean),
                            BeanReferenceSearchRequestor.RELEVANCE);
                }

                request.addProposal(proposal);
                beans.add(key);
            }
        }
    }

    public void acceptSearchMatch(String beanId, Node beanNode, IFile file, String prefix) {
        NamedNodeMap attributes = beanNode.getAttributes();
        if (beanId.toLowerCase().startsWith(prefix.toLowerCase())) {
            if (beanNode.getParentNode() != null
                    && "beans".equals(beanNode.getParentNode().getNodeName())) {
                String beanName = beanId;
                String replaceText = beanName;
                String fileName = file.getProjectRelativePath().toString();
                String key = beanName + fileName;
                if (!beans.contains(key)) {
                    StringBuffer buf = new StringBuffer();
                    buf.append(beanName);
                    if (attributes.getNamedItem("class") != null) {
                        String className = attributes.getNamedItem("class").getNodeValue();
                        buf.append(" [");
                        buf.append(Signature.getSimpleName(className));
                        buf.append("]");
                    }
                    if (attributes.getNamedItem("parent") != null) {
                        String parentName = attributes.getNamedItem("parent").getNodeValue();
                        buf.append(" <");
                        buf.append(parentName);
                        buf.append(">");
                    }
                    buf.append(" - ");
                    buf.append(fileName);
                    String displayText = buf.toString();
                    Image image = new DelegatingLabelProvider().getImage(beanNode);

                    BeansJavaCompletionProposal proposal = null;

                    String className = BeansEditorUtils.getClassNameForBean(beanNode);
                    if (this.requiredTypes.contains(className)) {
                        proposal = new BeansJavaCompletionProposal(replaceText, request
                                .getReplacementBeginPosition(), request.getReplacementLength(),
                                replaceText.length(), image, displayText, null, BeansEditorUtils
                                        .createAdditionalProposalInfo(beanNode, file),
                                TYPE_MATCHING_RELEVANCE);
                    }
                    else {
                        proposal = new BeansJavaCompletionProposal(replaceText, request
                                .getReplacementBeginPosition(), request.getReplacementLength(),
                                replaceText.length(), image, displayText, null, BeansEditorUtils
                                        .createAdditionalProposalInfo(beanNode, file), RELEVANCE);
                    }

                    request.addProposal(proposal);
                    beans.add(key);
                }
            }
        }
    }
}