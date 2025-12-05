#!/usr/bin/env bash
# Builds viewmapper-agent jar and viewmapper-mcp-server container

set -e  # Exit on error
set -u  # Exit on undefined variable

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_DIR="${PROJECT_ROOT}/viewmapper-agent"
MCP_SERVER_DIR="${PROJECT_ROOT}/viewmapper-mcp-server"
JAR_NAME="viewmapper-478.jar"
DOCKER_IMAGE="viewmapper:478"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed. Please install Java first."
        exit 1
    fi
    log_success "All prerequisites satisfied"
}

# Build viewmapper-agent
build_agent() {
    log_info "Building viewmapper-agent..."
    cd "${AGENT_DIR}"
    if mvn clean package; then
        log_success "viewmapper-agent built successfully"
    else
        log_error "Failed to build viewmapper-agent"
        exit 1
    fi
    # Verify JAR exists
    if [ ! -f "${AGENT_DIR}/target/${JAR_NAME}" ]; then
        log_error "JAR file not found at ${AGENT_DIR}/target/${JAR_NAME}"
        exit 1
    fi
    cd "${PROJECT_ROOT}"
}

# Copy JAR to MCP server directory
copy_jar() {
    log_info "Copying JAR to viewmapper-mcp-server..."
    local source="${AGENT_DIR}/target/${JAR_NAME}"
    local dest="${MCP_SERVER_DIR}/${JAR_NAME}"
    if cp "${source}" "${dest}"; then
        log_success "JAR copied to ${dest}"
    else
        log_error "Failed to copy JAR"
        exit 1
    fi
}

# Build Docker container
build_docker() {
    log_info "Building Docker container..."
    cd "${MCP_SERVER_DIR}"

    # Remove existing image if present
    if docker image inspect "${DOCKER_IMAGE}" &> /dev/null; then
        log_warning "Removing existing Docker image ${DOCKER_IMAGE}..."
        docker image rm -f "${DOCKER_IMAGE}"
    fi

    # Build new image
    if docker build --no-cache -t "${DOCKER_IMAGE}" .; then
        log_success "Docker container built successfully: ${DOCKER_IMAGE}"
    else
        log_error "Failed to build Docker container"
        exit 1
    fi

    cd "${PROJECT_ROOT}"
}

# Display build summary
display_summary() {
    echo ""
    echo "=========================================="
    log_success "viewmapper-478 is go 🚀"
    echo "=========================================="
    echo ""
    echo "Build artifacts:"
    echo "  - JAR: ${AGENT_DIR}/target/${JAR_NAME}"
    echo "  - JAR (copied): ${MCP_SERVER_DIR}/${JAR_NAME}"
    echo "  - Docker image: ${DOCKER_IMAGE}"
    echo ""
    echo "Next steps:"
    echo "  1. Configure Claude Desktop to use the Docker container"
    echo "  2. See README.md for configuration examples"
    echo ""
}

# Main execution
main() {
    log_info "Starting build process..."
    check_prerequisites
    build_agent
    copy_jar
    build_docker
    display_summary
}

# Run main function
main
