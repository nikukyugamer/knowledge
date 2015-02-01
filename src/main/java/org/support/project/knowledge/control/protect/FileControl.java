package org.support.project.knowledge.control.protect;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.support.project.common.log.Log;
import org.support.project.common.log.LogFactory;
import org.support.project.di.DI;
import org.support.project.di.Instance;
import org.support.project.knowledge.control.Control;
import org.support.project.knowledge.dao.KnowledgeFilesDao;
import org.support.project.knowledge.entity.KnowledgeFilesEntity;
import org.support.project.knowledge.logic.UploadedFileLogic;
import org.support.project.knowledge.vo.UploadFile;
import org.support.project.knowledge.vo.UploadResults;
import org.support.project.web.boundary.Boundary;
import org.support.project.web.boundary.JsonBoundary;
import org.support.project.web.common.HttpStatus;

@DI(instance=Instance.Prototype)
public class FileControl extends Control {
	/** ログ */
	private static Log LOG = LogFactory.getLog(FileControl.class);
	
	private KnowledgeFilesDao filesDao = KnowledgeFilesDao.get();
	private UploadedFileLogic fileLogic = UploadedFileLogic.get();
	
	/**
	 * アップロードされたファイルを保存する
	 * @return
	 * @throws Exception
	 */
	public Boundary upload() throws Exception {
		UploadResults results = new UploadResults();
		List<UploadFile> files = new ArrayList<UploadFile>();
		Object obj = getParam("files[]", Object.class);
		if (obj instanceof FileItem) {
			FileItem fileItem = (FileItem) obj;
			UploadFile file = fileLogic.saveFile(fileItem, getLoginedUser(), getRequest().getContextPath());
			files.add(file);
		} else if (obj instanceof List) {
			List<FileItem> fileItems = (List<FileItem>) obj;
			for (FileItem fileItem : fileItems) {
				UploadFile file = fileLogic.saveFile(fileItem, getLoginedUser(), getRequest().getContextPath());
				files.add(file);
			}
		}
		results.setFiles(files);
		return send(HttpStatus.SC_200_OK, results);
	}
	
	
	
	public JsonBoundary delete() {
		LOG.trace("delete()");
		
		Long fileNo = getParam("fileNo", Long.class);
		KnowledgeFilesEntity entity = filesDao.selectOnKeyWithoutBinary(fileNo);
		if (entity == null) {
			// 既に削除済
			return send(HttpStatus.SC_200_OK, "success: " + fileNo);
		}
		if (0 == entity.getKnowledgeId() || entity.getKnowledgeId() == null) {
			// ナレッジと紐づいていないものなので削除してOK
			// 紐づいているものは、ナレッジを更新した際に、紐付きがなければ削除になるので、実際の削除処理は実施しない
			if (!getLoginedUser().isAdmin()) {
				if (entity.getInsertUser().intValue() != getLoginUserId().intValue())  {
					// 登録者以外に削除はさせない
					return send(HttpStatus.SC_400_BAD_REQUEST, "fail: " + fileNo);
				}
			}
			// 削除実行
			fileLogic.removeFile(fileNo, getLoginedUser());
		}
		return send(HttpStatus.SC_200_OK, "success: " + fileNo);
	}
	
	
}