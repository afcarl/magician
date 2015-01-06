package starlib.mln.infer.store.internal.be;

import starlib.mln.infer.store.internal.IntFunction;

public class BucketChannelInt {
	
	IntBucket sender;
	
	IntBucket receiver;
	
	private IntFunction message;
	
	public BucketChannelInt(IntBucket sender, IntBucket receiver) {
		this.sender = sender;
		this.receiver = receiver;
		receiver.addInputChannel(this);
	}
	
	public void setMessage(IntFunction message) {
		this.message = message;
	}
	
	public IntFunction getMessage() {
		return message;
	}

}
