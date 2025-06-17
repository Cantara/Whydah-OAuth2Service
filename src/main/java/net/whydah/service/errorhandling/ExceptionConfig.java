package net.whydah.service.errorhandling;


import net.whydah.util.Configuration;

public class ExceptionConfig {
	
	public enum ErrorLevel{
		SHOW_ALL (0),
		SHOW_ALL_BUT_NO_STACKTRACE (1),
		SHOW_NO_DEVELOPER_MESSAGE_AND_EXCEPTION_STACKTRACE (2);
		
		private final int level;
		
		ErrorLevel(int level) {
	      this.level = level;
	    }

		public int getLevel() {
			return level;
		}
		
		public ErrorMessage handleSecurityLevel(ErrorMessage error){
			if(level == 0){ //no constraint
				return error;
			} else if(level == 1){
				if(error.getCode()== 9999){
					error.setErrorDescription("");
				}
			} else if(level == 2){
				error.setErrorDescription("");
			}
			return error;
		}
		
	}
	
	//SET THE HIGHEST SECURITY AS DEFAULT
    public static final ErrorLevel level;
	
	static {
		String error = Configuration.getString("errorlevel");
        if(error !=null){
            if(error.trim().equals("0")){
                level = ErrorLevel.SHOW_ALL;
            } else if (error.trim().equals("1")){
                level = ErrorLevel.SHOW_ALL_BUT_NO_STACKTRACE;
            } else if (error.trim().equals("2")){
                level = ErrorLevel.SHOW_NO_DEVELOPER_MESSAGE_AND_EXCEPTION_STACKTRACE;
            } else {
                level = ErrorLevel.SHOW_NO_DEVELOPER_MESSAGE_AND_EXCEPTION_STACKTRACE;
            }
        } else {
            level = ErrorLevel.SHOW_NO_DEVELOPER_MESSAGE_AND_EXCEPTION_STACKTRACE;

        }
		
	}
	
	public static ErrorMessage handleSecurity(ErrorMessage error){
		return level.handleSecurityLevel(error);
	}
	

}
