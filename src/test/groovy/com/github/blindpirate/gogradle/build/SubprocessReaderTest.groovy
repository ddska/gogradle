package com.github.blindpirate.gogradle.build

import com.github.blindpirate.gogradle.GogradleRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyInt
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@RunWith(GogradleRunner)
class SubprocessReaderTest {

    @Mock
    Consumer consumer
    @Mock
    Supplier supplier
    @Mock
    InputStream inputStream

    @Test
    void 'countdown latch should terminate when exception occurs'() {
        CountDownLatch latch = new CountDownLatch(1)
        // NPE will be thrown
        new SubprocessReader(null, null, latch).start()
        latch.await()
    }

    @Test
    void 'input stream should be consumed line by line'() {
        CountDownLatch latch = new CountDownLatch(1)
        new SubprocessReader(
                new Supplier<InputStream>() {
                    @Override
                    InputStream get() {
                        return new ByteArrayInputStream('1\n2'.getBytes('UTF8'))
                    }
                },
                consumer,
                latch).run()
        latch.await()
        verify(consumer).accept('1')
        verify(consumer).accept('2')
    }

    @Test
    void 'stacktrace of exception should be consumed'() {
        // given
        when(supplier.get()).thenReturn(inputStream)
        when(inputStream.read(any(byte[]), anyInt(), anyInt())).thenThrow(new IOException())
        // when
        CountDownLatch latch = new CountDownLatch(1)
        new SubprocessReader(supplier, consumer, latch).start()
        latch.await(1, TimeUnit.SECONDS)
        // then
        ArgumentCaptor captor = ArgumentCaptor.forClass(String)
        verify(consumer).accept(captor.capture())
        assert captor.getValue().contains('java.io.IOException')
    }
}
