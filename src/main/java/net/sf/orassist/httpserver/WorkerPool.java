package net.sf.orassist.httpserver;

import java.util.ArrayList;
import java.util.function.Consumer;

public class WorkerPool<T> {
	private ArrayList<Worker<T>> used = new ArrayList<>();
	private ArrayList<Worker<T>> idle = new ArrayList<>();
	@SuppressWarnings("unchecked")
	public WorkerPool(int limit, @SuppressWarnings("rawtypes") Class procClass) throws InstantiationException, IllegalAccessException {
		for (int i = 0; i < limit; i ++) {
			idle.add(new Worker<T>(this, (Consumer<T>) procClass.newInstance()));
		}
	}
	public Worker<T> alloc() throws InterruptedException {
		Worker<T> ret = null;
		while (ret == null) {
			while (idle.size() <= 0)
				Thread.sleep(100);
			synchronized(this) {
				if (idle.size() > 0) {
					ret = idle.remove(idle.size() - 1);
					used.add(ret);
				}
			}
		}
		return ret;
	}
	public synchronized void release(Worker<T> t) {
		used.remove(t);
		idle.add(t);
	}
	public void setWorkload(T workload) throws InterruptedException {
		Worker<T> worker = alloc();
		worker.setWorkload(workload);
		worker.getThread().interrupt();
	}

	public static class Worker<T> implements Runnable {

		private T workload = null;
		protected Thread thread;
		protected WorkerPool<T> pool;
		private Consumer<T> consumer;


		public void setWorkload(T workload) {
			synchronized(this) {
				this.workload = workload;
			}
		}

		public T getWorkload() {
			synchronized(this) {
				return workload;
			}
		}

		public void run() {
			while (true) {
				if (getWorkload() != null) {
					consumer.accept(workload);
					setWorkload(null);
					pool.release(this);
				}
				try {
					Thread.sleep(65535);
				} catch (InterruptedException e) {
					e.getMessage();
				}
			}
		}

		public Thread getThread() {
			return thread;
		}

		public Worker(WorkerPool<T> pool, Consumer<T> consumer) {
			this.consumer = consumer;
			this.thread = new Thread(this);
			this.pool = pool;
			thread.start();
		}
	}
}
