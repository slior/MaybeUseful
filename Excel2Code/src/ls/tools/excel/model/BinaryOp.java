package ls.tools.excel.model;

import fj.data.List;
import ls.tools.excel.CellType;

public enum BinaryOp implements Function
{

	MULT {
		@Override
		public List<Param> parameters()
		{
//			return List.<Param>list()
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Method parameters is not implemented yet in Function");
		}

		@Override
		public Expr body()
		{
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Method body is not implemented yet in Function");
		}

		@Override
		public CellType returnType()
		{
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Method returnType is not implemented yet in Function");
		}
		
	}
	
}
