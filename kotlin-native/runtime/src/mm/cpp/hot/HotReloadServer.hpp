//
// Created by Gabriele.Pappalardo on 25/07/2025.
//

#ifndef HOTRELOADSERVER_HPP
#define HOTRELOADSERVER_HPP

#include "CompilerConstants.hpp"

#include <iostream>
#include <vector>
#include <string>
#include <thread>
#include <string_view>

#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "HotReloadUtility.hpp"

using namespace kotlin::hot;

class HotReloadServer {
public:
    explicit HotReloadServer(
            const int32_t port = (kotlin::compiler::hotReloadServerPort() == -1) ? kDefaultServerPort
                                                                             : kotlin::compiler::hotReloadServerPort()) :
        port(port) {}

    ~HotReloadServer() { stop(); }

    bool start() {
        // Create socket
        serverFd = socket(AF_INET, SOCK_STREAM, 0);
        if (serverFd == -1) {
            utility::log("Failed to create socket", utility::LogLevel::ERR);
            return false;
        }

        // Set socket options to reuse address
        int opt = 1;
        if (setsockopt(serverFd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
            utility::log("Failed to set socket options", utility::LogLevel::ERR);
            close(serverFd);
            return false;
        }

        // Bind to localhost
        sockaddr_in address;
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = inet_addr(kServerEndpoint);
        address.sin_port = htons(port);

        if (bind(serverFd, reinterpret_cast<sockaddr*>(&address), sizeof(address)) < 0) {
            utility::log("Failed to bind to port 127.0.0.1:" + std::to_string(port), utility::LogLevel::ERR);
            close(serverFd);
            return false;
        }

        // Listen for connections
        if (listen(serverFd, 3) < 0) {
            utility::log("Failed to listen on socket", utility::LogLevel::ERR);
            close(serverFd);
            return false;
        }

        running = true;
        utility::log("HotReloadServer listening on localhost:" + std::to_string(port));
        return true;
    }

    template <typename F>
    void run(F&& onReloadMessageCallback) {
        /// We can only handle one request at time (for the moment).

        runningThread = std::make_unique<std::thread>([this, onReloadMessageCallback]() {
#if KONAN_APPLE
            pthread_setname_np("HotReload Server Listener Thread");
#endif

            if (!running) {
                utility::log("Server not started", utility::LogLevel::ERR);
                return;
            }

            sockaddr_in clientAddress;
            socklen_t clientLen = sizeof(clientAddress);

            while (running) {
                const int clientSocket = accept(serverFd, reinterpret_cast<struct sockaddr*>(&clientAddress), &clientLen);
                if (clientSocket < 0) {
                    if (running) {
                        utility::log("Failed to accept connection, next...", utility::LogLevel::ERR);
                    }
                    continue;
                }

                utility::log("Accepting incoming client");
                handleReloadMessage(clientSocket, onReloadMessageCallback);
                close(clientSocket);
            }
        });
    }

    void stop() {
        if (running) {
            running = false;
            if (serverFd != -1) {
                close(serverFd);
                serverFd = -1;
            }
            utility::log("Stopping HotReload server...");
            if (runningThread != nullptr) {
                runningThread->join();
            }
        }
    }

private:
    static constexpr auto kServerEndpoint = "127.0.0.1";
    static constexpr auto kDefaultServerPort = 5567;

    int serverFd{0};
    int32_t port{0};
    bool running{false};
    std::unique_ptr<std::thread> runningThread{nullptr};

    static bool readExact(const int socket, void* buffer, const size_t bytes) {
        size_t totalRead = 0;
        char* buf = static_cast<char*>(buffer);

        while (totalRead < bytes) {
            const ssize_t readBytes = recv(socket, buf + totalRead, bytes - totalRead, 0);
            if (readBytes <= 0) {
                return false; // Connection closed or error
            }
            totalRead += readBytes;
        }
        return true;
    }

    template <typename F>
    static void handleReloadMessage(const int clientSocket, F&& onReloadCallback) {
        uint32_t numDylibs{0};
        if (!readExact(clientSocket, &numDylibs, sizeof(numDylibs))) {
            log("Failed to read number of dylibs", utility::LogLevel::ERR);
            return;
        }

        // Read each dylib path
        std::vector<std::string> dylibPaths;
        for (uint32_t i = 0; i < numDylibs; i++) {
            // Read path length
            uint32_t pathLength;
            if (!readExact(clientSocket, &pathLength, sizeof(pathLength))) {
                utility::log("Failed to read path length for dylib " + std::to_string(i), utility::LogLevel::ERR);
                return;
            }

            // Read path string
            std::vector<char> pathBuffer(pathLength);
            if (!readExact(clientSocket, pathBuffer.data(), pathLength)) {
                utility::log("Failed to read path for dylib " + std::to_string(i), utility::LogLevel::ERR);
                return;
            }

            std::string path(pathBuffer.data(), pathLength);
            // Remove null terminator if present
            if (!path.empty() && path.back() == '\0') {
                path.pop_back();
            }

            dylibPaths.push_back(path);
            utility::log("dylib " + std::to_string(i) + ": " + path, utility::LogLevel::INFO);
        }

        onReloadCallback(dylibPaths);
    }
};

#endif // HOTRELOADSERVER_HPP
