package cl.beauty.util.docs;

import java.util.ArrayList;
import java.util.List;

public class TableHeader {
		private String fieldName;
		private String headerName;
		private String type ="text";
		private String render="";
		
		private List<TableHeader> subHeaders = new ArrayList<TableHeader>();
		
		public TableHeader(String fieldName, String headerName) {
			this.setFieldName(fieldName);
			this.setHeaderName(headerName);
		}
		public TableHeader(String fieldName, String headerName,String type) {
			this.setFieldName(fieldName);
			this.setHeaderName(headerName);
			this.type = type;
		}
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public String getHeaderName() {
			return headerName;
		}
		public void setHeaderName(String headerName) {
			if(headerName != null && !headerName.equals(""))
				this.headerName = headerName;
			else
				this.headerName = this.fieldName;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public List<TableHeader> getSubHeaders() {
			return subHeaders;
		}
		public void setSubHeaders(List<TableHeader> subHeaders) {
			this.subHeaders = subHeaders;
		}
		public String getRender() {
			return render;
		}
		public void setRender(String render) {
			this.render = render;
		}
	}
