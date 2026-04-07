package sd2526.trab.clients.java;

import sd2526.trab.Discovery;

public interface ClientFactory<T> {

    T get(String domain, Discovery discovery) ;
}
