//
// Created by Gabriele.Pappalardo on 25/07/2025.
//

#ifndef HOTRELOADSERVER_HPP
#define HOTRELOADSERVER_HPP

#include <iostream>
#include <vector>
#include <string>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <thread>
#include <string_view>

#include "HotReloadUtility.hpp"

using namespace kotlin::hot;

class HotReloadServer {
private:
    int serverFd{0};
    int port{0};
    bool running{false};
    std::unique_ptr<std::thread> runningThread{nullptr};

    // Read exactly n bytes from socket
    static bool ReadExact(const int socket, void* buffer, const size_t bytes) {
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
    static void HandleReloadMessage(const int clientSocket, F&& onReloadCallback) {
        uint32_t numDylibs{0};
        if (!ReadExact(clientSocket, &numDylibs, sizeof(numDylibs))) {
            log("Failed to read number of dylibs", utility::LogLevel::ERROR);
            return;
        }

        utility::log("Received RELOAD message with " + std::to_string(numDylibs) + " dylibs:", utility::LogLevel::INFO);

        // Read each dylib path
        std::vector<std::string> dylibPaths;
        for (uint32_t i = 0; i < numDylibs; i++) {
            // Read path length
            uint32_t pathLength;
            if (!ReadExact(clientSocket, &pathLength, sizeof(pathLength))) {
                utility::log("Failed to read path length for dylib " + std::to_string(i), utility::LogLevel::ERROR);
                return;
            }

            // Read path string
            std::vector<char> pathBuffer(pathLength);
            if (!ReadExact(clientSocket, pathBuffer.data(), pathLength)) {
                utility::log("Failed to read path for dylib " + std::to_string(i), utility::LogLevel::ERROR);
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

public:
    explicit HotReloadServer(const int port = 5565) : serverFd(0), port(port), running(false) {}

    ~HotReloadServer() { Stop(); }

    bool Start() {
        utility::InitializeHotReloadLogs(); // FIXME: I dunno why the compiler complains here

        // Create socket
        serverFd = socket(AF_INET, SOCK_STREAM, 0);
        if (serverFd == -1) {
            utility::log("Failed to create socket", utility::LogLevel::ERROR);
            return false;
        }

        // Set socket options to reuse address
        int opt = 1;
        if (setsockopt(serverFd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
            utility::log("Failed to set socket options", utility::LogLevel::ERROR);
            close(serverFd);
            return false;
        }

        // Bind to localhost
        sockaddr_in address;
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = inet_addr("127.0.0.1");
        address.sin_port = htons(port);

        if (bind(serverFd, reinterpret_cast<sockaddr*>(&address), sizeof(address)) < 0) {
            utility::log("Failed to bind to port " + std::to_string(port), utility::LogLevel::ERROR);
            close(serverFd);
            return false;
        }

        // Listen for connections
        if (listen(serverFd, 3) < 0) {
            utility::log("Failed to listen on socket", utility::LogLevel::ERROR);
            close(serverFd);
            return false;
        }

        running = true;
        utility::log("HotReload server listening on localhost:" + std::to_string(port));
        return true;
    }

    template <typename F>
    void Run(F&& onReloadMessageCallback) {
        /// We can only handle one request at time (for the moment).

        runningThread = std::make_unique<std::thread>([this, onReloadMessageCallback]() {
            pthread_setname_np("HotReload Server Listener Thread");

            if (!running) {
                utility::log("Server not started", utility::LogLevel::ERROR);
                return;
            }

            sockaddr_in clientAddress;
            socklen_t clientLen = sizeof(clientAddress);

            while (running) {
                const int clientSocket = accept(serverFd, reinterpret_cast<struct sockaddr*>(&clientAddress), &clientLen);
                if (clientSocket < 0) {
                    if (running) {
                        utility::log("Failed to accept connection, next...", utility::LogLevel::ERROR);
                    }
                    continue;
                }

                utility::log("Accepting incoming client");

                HandleReloadMessage(clientSocket, onReloadMessageCallback);

                close(clientSocket);
            }
        });
    }

    void Stop() {
        if (running) {
            running = false;
            if (serverFd != -1) {
                close(serverFd);
                serverFd = -1;
            }
            utility::log("Stopping listener server...");
            if (runningThread != nullptr) {
                runningThread->join();
            }
        }
    }
};

#endif // HOTRELOADSERVER_HPP
