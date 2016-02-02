# Streaming Compute Engine

## Introduction
One of Google's most significant achievements in its first decade was revolutionising the field of large-scale distributed systems with MapReduce, a robust framework for performing analytics on massive amounts of data in parallel without the need for high performance computers or supercomputers. This capability was seminal in developing and scaling Google Search and has since found countless uses in the software industry. However, a fundamental limitation of MapReduce is that it only supports batch processing and cannot be used to perform analytics on streams of data in real-time. Such a facility can be very useful in a wide range of fields such as computational fluid dynamics, real-time event processing and simulated or automated surgery, to name a few.

This project is one attempt at tackling that problem. Streaming Compute Engine is a Java framework for performing arbitrary distributed tasks on datasets over a cluster and updating results as a real-time stream rather than as a batch process. It is meant as a proof-of-concept style piece of software to demonstrate reliable, on-the-fly sequencing of processed data under unreliable network and processor conditions.

Some quick tests on a sample program which performs a Simpson integration on a sinusoidal function discretised into an 8000 point domain yielded the following results:

Total time for processing and network data transfer
    * with one remote worker: ~75s
    * with two remote workers: ~42s
    * with three remote workers: ~31s
    
NB the bulk of this time was expended during data transfer over the network. With a local cluster these times would be improved by orders of magnitude.

A short video demonstration of the same integration on 2000 data points can be found at <link>. All source is provided here for the framework as well as the sample program. The following contains a quick guide to implementing a program using the API, a detailed explanation of how the code works and an evaluation of its strengths and weaknesses as well as a discussion of future work.

## How to Use the Framework
To create a custom task, subclass UnitTask and implement the setSupplementaries(...) and execute(...) methods. The former allows you to provide each task with extra data points from outside the chunk on which it is operating and the latter defines the operation to be performed by a worker on the task's data chunk. Compile all class files for the custom task and dependencies which are not contained in the Java standard library. These must all be from the same package. ComputeClient assumes a gradle build so that the class files will be in said package under the bin directory.

To prepare the client, instantiate ComputeClient with the master IP and port and initialise it with the data, class files (in the form of local file URLs), chunk size (or 'unitSize') and any other required parameters. Start the client and call getResultUnit() in a loop until the ComputeClient.done() is true. To start the master, simply instantiate ComputeMaster with the worker IPs and call start(). Similarly, to start a worker instantiate ComputeWorker and call start(). A complete example can be seen in the provided sample. NB: the SampleClient program takes the master IP and port as arguments and the SampleMaster program takes the worker IPs as arguments. Detailed explanations of specifics can be found in the Javadoc.

## How the Code Works
The framework functions as a master-worker model. The client program sends data to the master which distributes it in chunks to workers in a round-robin manner. The number of data points in each chunk is specified in advance. As each worker produces result units, one per chunk, it streams these back to the master which sequences the incoming units on the fly using a construct we will refer to as a 'chain space'. The Java Reflection API is used to run dynamically downloaded user code on worker machines.

The starting points are the classes ComputeClient, ComputeMaster and ComputeWorker, which should each be run on separate machines (or on separate processes in the same machine for testing purposes). The ComputeClient sends a batch of indexed data (each data point could be of any type) as well as a custom task which is to operate on a chunk of data the size of which is also provided. In addition, any required parameters that are global across all chunks are given. If the custom task needs to use data points which are not contained in the chunk it is to operate on (for instance for boundary conditions) then these can be set in the custom task definition (as 'supplementaries'). Finally, the ComputeClient will send the class files for the custom task implementation (which must be a subclass of UnitTask) and any required classes that are not in the standard Java library.

Once all this information has been sent to the master node, the GenericProcessor class takes care of mapping the data to the workers. First, the data sequence is divided into chunks of equal size (specified by client). Each chunk is packaged into a TaskWrapper object with an instance of the provided UnitTask subclass. Then, the TaskWrapper instances are mapped into worker task queues, one per worker, and if each chunk needs access to external data points, these are given to its TaskWrapper. Once the task queues have been prepared, a dedicated thread is started for each worker (implementation in WorkerLinkThread) and this sends the task queues to its worker.

When a task queue is received on a worker machine, a new thread is started which calls execute() on each TaskWrapper, which in turn puts result units on a result queue. As this is happening, the main thread takes result units from the queue and sends them back to the appropriate WorkerLinkThread on the master machine. Note that all queues used are instances of SynchronizedQueue, a thread-safe implementation of the Java LinkedBlockingQueue.

On reception at the master, each worker thread dynamically puts its result units into the chain space which sequences result units on the fly. The chain space is a thread-safe data structure in which the following happens:
* the main thread in the master instantiates the chain space
* worker threads put their result units into the chain space, along with the position of each unit, encapsulated as Nodes
* when a node is inserted, it forms link(s) with one or both of the nodes before and after it respectively
* the chain space receives a request from the main thread to get a buffer of the next available result units
* if the next required node in the sequence is present, it and any nodes to which it is linked are sent out through the chain           space's port
* if the next required node in the sequence is not present, the main thread blocks until it is
* when the final node is passed in, it forms a link with the 'tail' which, when received by the port, signals that all data has         been sequenced and that the chain space can be closed by the main thread.
    
The chain space is the central component of this framework as it facilitates the reordering of result units as they arrive, taking into account that they are not expected to arrive in the sequence in which they were sent out due to the unpredictable nature of processing times and the network. It preserves correctness by waiting for the next required node to arrive and efficiency by linking together other nodes which have arrived early in the meantime. This linking into chains can be useful for speed, especially if the project is extended to support storing arriving nodes in a database to improve scalability. In this case, the number of queries could be reduced substantially.

The client requests result units in a loop until they have all been received. When the master receives a result request it requests a result buffer from the chain space and sends the next unit to the client, storing the other units from the buffer in a temporary result queue until another result request comes in from the client.

ComputeClient returns result units to the calling program where they can be processed to the user's liking. For example, the provided sample executes a Simpson's Rule integration on several chunks of input data (x-values) and then cumulatively adds up the result units as they arrive so that the final integral is obtained when the last result unit arrives.

## Contributions, Limitations and Future Work
The main contribution of this project is to provide a way to process datasets in parallel but return results as a stream rather than after all processing has finished. Notice however that only results come back as a stream, whereas data input is as of yet in batch form. In the future this will be amended to support streaming input, which will make it possible to use this framework in applications such as modeling blood flow in real-time to aid simulated surgery and other differential equation oriented operations. This will also solve another problem: the large amount of time taken in transferring data to the master and then to the workers without any results being returned in the meantime. Once the input is streamed to the master and workers, results can be returned concurrently.

As a barebones implementation of a distributed processing framework, this project does not have many of the features present in mature frameworks like MapReduce, the two most important being massive scalability and fault tolerance. At present, all data is stored in memory during processing which is obviously not ideal for scalability purposes. However, with support for input streaming, scalability will not be so much of a problem since memory will be reused as data units come and go. Moreover, as no data is stored on the file system of any node, disk sizes do not matter. In the future, database support could be introduced to further improve scalability.

Fault tolerance is not present in any significant form here but was not part of the scope of this project. This framework is rather a proof of concept of the idea of dynamically sequencing incoming processed data from multiple sources, as demonstrated by the ChainSpace class. Fault tolerance could be added by caching data units until their results are in the chain space and re-issuing failed tasks. The extra time taken would not be a threat to correctness of order as the chain space already takes variable arrival times into account.

The code also contains an implementation of a dynamic code downloader which is used to transfer the task and dependency classes. This component is comparable in functionality to Java RMI but is simpler and does not contain any of the security features as, again, a highly robust code downloading framework is out of the scope of this project.

Lastly, since only class files are transfered and not source, all provided classes must be in the same package, which could be inconvenient if third-party libraries are used. This could be solved by transferring source and dynamically compiling on the other side.

## Conclusion
This project works well as a first step towards a scalable streaming processor but there are plenty of areas which could be improved or extended. Foremost, input streaming needs to be a feature as without this the main goal of a framework like this is not achieved. Fortunately, this will not be a major undertaking as there is no sequencing to be done on the way in.
